package com.malvinas.rutas.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VehiculosServiceClient {
    private final RestClient restClient;

    public VehiculosServiceClient(@Qualifier("lbBuilder") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://vehiculos-service").build();
    }

    @CircuitBreaker(name = "vehiculos-service", fallbackMethod = "changeStatusFallback")
    public void changeVehicleStatus(String plate, String statusCode, String reason) {
        restClient.put().uri("/api/vehicles/{plate}/status", plate)
                .body(new StatusChangeRequest(statusCode, reason, null))
                .retrieve().toBodilessEntity();
    }

    public void changeStatusFallback(String plate, String statusCode, String reason, Throwable t) {}

    public record StatusChangeRequest(String newStatusCode, String reason, Long employeeId) {}
}
