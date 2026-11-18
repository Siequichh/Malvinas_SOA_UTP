package com.malvinas.rutas.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    @Qualifier("lbBuilder")
    public RestClient.Builder lbRestClientBuilder(@Value("${internal.key}") String internalKey) {
        // Toda llamada entre servicios lleva el secreto compartido (ver InternalKeyFilter del destino)
        return RestClient.builder()
                .defaultHeader("X-Internal-Key", internalKey)
                .requestInterceptor(propagateEmployee());
    }

    /** Reenvia la identidad del usuario: vehiculos-service la registra como actor en el historial de estados. */
    private static ClientHttpRequestInterceptor propagateEmployee() {
        return (request, body, execution) -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                for (String h : new String[]{"X-Employee-Id", "X-Employee-Role", "X-Employee-Name"}) {
                    String v = attrs.getRequest().getHeader(h);
                    if (v != null && !v.isBlank()) request.getHeaders().set(h, v);
                }
            }
            return execution.execute(request, body);
        };
    }
}
