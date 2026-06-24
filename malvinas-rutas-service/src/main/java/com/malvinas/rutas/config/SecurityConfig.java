package com.malvinas.rutas.config;

import com.malvinas.rutas.infrastructure.security.RequestHeaderAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
                    "/api-docs/**", "/v3/api-docs/**"
                ).permitAll()
                // Lectura: acceso libre para llamadas internas (reportes-service)
                .requestMatchers(HttpMethod.GET, "/api/dispatches/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/delivery-points/**").permitAll()
                // Gestion de despachos: conductores pueden registrar salida/llegada, supervisores gestionan
                .requestMatchers("/api/dispatches/**").hasAnyRole("ADM", "SUP", "DRV")
                // Gestion de puntos de entrega: solo administradores y supervisores
                .requestMatchers("/api/delivery-points/**").hasAnyRole("ADM", "SUP")
                // Cualquier otra solicitud requiere autenticacion
                .anyRequest().authenticated()
            )
            .addFilterBefore(requestHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
