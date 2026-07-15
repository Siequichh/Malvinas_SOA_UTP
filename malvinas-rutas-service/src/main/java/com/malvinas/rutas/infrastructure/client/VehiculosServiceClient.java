package com.malvinas.rutas.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VehiculosServiceClient {
    private static final Logger log = LoggerFactory.getLogger(VehiculosServiceClient.class);
    private final RestClient restClient;

    public VehiculosServiceClient(@Qualifier("lbBuilder") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://vehiculos-service").build();
    }

    /** Llamada con falla estricta — lanza si vehiculos no responde (ej. accept) */
    @CircuitBreaker(name = "vehiculos-service", fallbackMethod = "changeStatusFallback")
    public void changeVehicleStatus(String plate, String statusCode, String reason) {
        restClient.put().uri("/api/vehicles/{plate}/status", plate)
                .body(new StatusChangeRequest(statusCode, reason, null))
                .retrieve().toBodilessEntity();
    }

    /** Llamada best-effort — loguea y sigue si vehiculos falla (ej. complete) */
    @CircuitBreaker(name = "vehiculos-service", fallbackMethod = "changeStatusSilentFallback")
    public void changeVehicleStatusBestEffort(String plate, String statusCode, String reason) {
        restClient.put().uri("/api/vehicles/{plate}/status", plate)
                .body(new StatusChangeRequest(statusCode, reason, null))
                .retrieve().toBodilessEntity();
    }

    public void changeStatusFallback(String plate, String statusCode, String reason, Throwable t) {
        log.error("vehiculos-service unreachable — cannot update vehicle status plate={} code={}: {}", plate, statusCode, t.getMessage());
        throw new RuntimeException("Vehicle status update failed: " + t.getMessage(), t);
    }

    public void changeStatusSilentFallback(String plate, String statusCode, String reason, Throwable t) {
        log.error("vehiculos-service failed (best-effort) — plate={} code={}: {}", plate, statusCode, t.getMessage());
    }

    public record StatusChangeRequest(String newStatusCode, String reason, Long employeeId) {}
}
