package com.malvinas.rutas.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PersonalServiceClient {
    private final RestClient restClient;

    public PersonalServiceClient(@Qualifier("lbBuilder") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://personal-service").build();
    }

    @CircuitBreaker(name = "personal-service", fallbackMethod = "isActiveFallback")
    public boolean isEmployeeActive(Long employeeId) {
        var response = restClient.get()
                .uri("/api/employees/{id}/active", employeeId)
                .retrieve()
                .toEntity(ActiveResponse.class)
                .getBody();
        return response != null && Boolean.TRUE.equals(response.active());
    }

    public boolean isActiveFallback(Long employeeId, Throwable t) {
        return true; // tolerante: si personal-service no responde, no bloqueamos
    }

    public record ActiveResponse(Boolean active) {}
}
