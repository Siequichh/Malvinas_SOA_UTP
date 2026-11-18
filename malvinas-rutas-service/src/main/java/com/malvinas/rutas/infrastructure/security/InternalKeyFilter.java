package com.malvinas.rutas.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Solo acepta requests que traigan el secreto compartido X-Internal-Key (lo agrega el API Gateway
 * y los clientes entre servicios). Sin esto, cualquiera podria llamar al servicio directo por su
 * URL publica y falsificar X-Employee-Role.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";
    private final byte[] key;

    public InternalKeyFilter(@Value("${internal.key}") String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || p.startsWith("/actuator/")
                || p.startsWith("/swagger-ui") || p.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        if (given == null || !MessageDigest.isEqual(key, given.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\"Acceso solo a traves del API Gateway\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
