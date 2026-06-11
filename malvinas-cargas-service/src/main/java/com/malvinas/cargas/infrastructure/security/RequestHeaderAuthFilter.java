package com.malvinas.cargas.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads authentication context from headers forwarded by the API Gateway.
 * The Gateway validates the JWT and propagates:
 *   X-Employee-Id   → employee identifier (subject)
 *   X-Employee-Role → role code (e.g. ADM, SUP, MOV, DRV, SEC)
 *   X-Employee-Name → display name
 */
@Slf4j
@Component
public class RequestHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String employeeId = request.getHeader("X-Employee-Id");
        String role       = request.getHeader("X-Employee-Role");

        if (employeeId != null && role != null) {
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(employeeId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated employee={} role={}", employeeId, role);
        }

        filterChain.doFilter(request, response);
    }
}
