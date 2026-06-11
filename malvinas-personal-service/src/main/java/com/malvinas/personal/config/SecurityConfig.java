package com.malvinas.personal.config;

import com.malvinas.personal.infrastructure.security.RequestHeaderAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RequestHeaderAuthFilter requestHeaderAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Infraestructura y documentacion: acceso libre
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**", "/api-docs/**"
                ).permitAll()
                // Endpoints internos consumidos por auth-service (sin JWT)
                .requestMatchers(HttpMethod.GET, "/api/employees/by-dni/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/employees/auth/by-dni/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/employees/*/active").permitAll()
                // Lectura de roles: cualquier empleado autenticado
                .requestMatchers(HttpMethod.GET, "/api/roles/**").hasAnyRole("ADM", "SUP", "MOV", "DRV", "SEC")
                // Gestion de empleados: solo administradores
                .requestMatchers(HttpMethod.GET, "/api/employees/**").hasAnyRole("ADM", "SUP")
                .requestMatchers("/api/employees/**").hasRole("ADM")
                // Asistencia: supervisores y el propio empleado (via Gateway)
                .requestMatchers("/api/attendances/**").hasAnyRole("ADM", "SUP")
                // Cualquier otra solicitud requiere autenticacion
                .anyRequest().authenticated()
            )
            .addFilterBefore(requestHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
