package com.malvinas.auth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

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
        return RestClient.builder().defaultHeader("X-Internal-Key", internalKey);
    }
}
