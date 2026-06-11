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
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    private final JwtProperties jwtProperties;

    public JwtAuthFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
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
        if (path.startsWith("/api/auth/") || path.startsWith("/actuator/")) {
            chain.doFilter(request, response);
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

            String employeeId   = decoded.getSubject();
            String employeeRole = decoded.getClaim("role").asString();
            String employeeName = decoded.getClaim("name").asString();

            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                private final Map<String, String> extra = Map.of(
                        "X-Employee-Id",   employeeId   != null ? employeeId   : "",
                        "X-Employee-Role", employeeRole != null ? employeeRole : "",
                        "X-Employee-Name", employeeName != null ? employeeName : ""
                );
                @Override public String getHeader(String name) {
                    String v = extra.get(name); return v != null ? v : super.getHeader(name);
                }
                @Override public Enumeration<String> getHeaders(String name) {
                    String v = extra.get(name);
                    return v != null ? Collections.enumeration(Collections.singletonList(v)) : super.getHeaders(name);
                }
                @Override public Enumeration<String> getHeaderNames() {
                    Map<String, String> all = new HashMap<>(extra);
                    Enumeration<String> orig = super.getHeaderNames();
                    while (orig.hasMoreElements()) all.put(orig.nextElement(), "");
                    return Collections.enumeration(all.keySet());
                }
            };
            chain.doFilter(wrapped, response);

        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
