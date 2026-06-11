package com.malvinas.rutas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class AuditConfig {

    /**
     * Retorna el ID del empleado autenticado como auditor.
     * El ID viene del header X-Employee-Id propagado por el API Gateway.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principal) {
                return Optional.of(principal);
            }
            return Optional.of("system");
        };
    }
}
