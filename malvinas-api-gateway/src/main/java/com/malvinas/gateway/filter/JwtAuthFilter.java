package com.malvinas.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.malvinas.gateway.config.JwtProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Valida el JWT y reenvia la identidad del empleado a los servicios.
 * - Siempre agrega X-Internal-Key: los servicios rechazan lo que no venga del gateway.
 * - Nunca deja pasar X-Employee-* enviados por el cliente (se pisan o se ocultan).
 * - Bloquea endpoints internos de personal-service (solo los usa auth-service).
 */
@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
    private static final Pattern INTERNAL_ONLY =
            Pattern.compile("^/api/employees/(auth/.*|[^/]+/active/?)$");

    private final JwtProperties jwtProperties;
    private final String internalKey;

    public JwtAuthFilter(JwtProperties jwtProperties, @Value("${internal.key}") String internalKey) {
        this.jwtProperties = jwtProperties;
        this.internalKey = internalKey;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // OPTIONS preflight must pass through so CorsFilter can add the required headers
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }
        if (INTERNAL_ONLY.matcher(path).matches()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (path.startsWith("/api/auth/")) {
            chain.doFilter(withHeaders(request, Map.of(INTERNAL_KEY_HEADER, internalKey)), response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .build().verify(authHeader.substring(7));
            if ("refresh".equals(decoded.getClaim("type").asString())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // refresh token no sirve como access
                return;
            }
            chain.doFilter(withHeaders(request, Map.of(
                    INTERNAL_KEY_HEADER, internalKey,
                    "X-Employee-Id",   nz(decoded.getSubject()),
                    "X-Employee-Role", nz(decoded.getClaim("role").asString()),
                    "X-Employee-Name", nz(decoded.getClaim("name").asString())
            )), response);
        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private static String nz(String v) { return v != null ? v : ""; }

    /** Reemplaza/agrega headers y oculta cualquier X-Employee-* / X-Internal-Key que no pusimos nosotros. */
    private static HttpServletRequest withHeaders(HttpServletRequest request, Map<String, String> extra) {
        Map<String, String> byLower = new HashMap<>();
        extra.forEach((k, v) -> byLower.put(k.toLowerCase(), v));
        return new HttpServletRequestWrapper(request) {
            private boolean hidden(String name) {
                String n = name.toLowerCase();
                return !byLower.containsKey(n) && (n.startsWith("x-employee-") || n.equals("x-internal-key"));
            }
            @Override public String getHeader(String name) {
                String v = byLower.get(name.toLowerCase());
                if (v != null) return v;
                return hidden(name) ? null : super.getHeader(name);
            }
            @Override public Enumeration<String> getHeaders(String name) {
                String v = byLower.get(name.toLowerCase());
                if (v != null) return Collections.enumeration(Collections.singletonList(v));
                return hidden(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
            }
            @Override public Enumeration<String> getHeaderNames() {
                Set<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                all.addAll(extra.keySet());
                Enumeration<String> orig = super.getHeaderNames();
                while (orig.hasMoreElements()) {
                    String n = orig.nextElement();
                    if (!hidden(n)) all.add(n);
                }
                return Collections.enumeration(all);
            }
        };
    }
}
