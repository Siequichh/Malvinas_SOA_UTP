package com.malvinas.reportes.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AggregatorClient {

    private final RestClient vehiculosClient;
    private final RestClient cargasClient;
    private final RestClient rutasClient;

    public AggregatorClient(@Qualifier("lbBuilder") RestClient.Builder builder) {
        RestClient base = builder.build();
        this.vehiculosClient = base.mutate().baseUrl("http://vehiculos-service").build();
        this.cargasClient = base.mutate().baseUrl("http://cargas-service").build();
        this.rutasClient = base.mutate().baseUrl("http://rutas-service").build();
    }

    @CircuitBreaker(name = "vehiculos-service", fallbackMethod = "getVehiclesFallback")
    public List<Map<String, Object>> getVehicles() {
        return vehiculosClient.get().uri("/api/vehicles")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    @CircuitBreaker(name = "cargas-service", fallbackMethod = "getActiveLoadsFallback")
    public List<Map<String, Object>> getActiveLoads() {
        return cargasClient.get().uri("/api/loads/active")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    @CircuitBreaker(name = "rutas-service", fallbackMethod = "getActiveDispatchesFallback")
    public List<Map<String, Object>> getActiveDispatches() {
        return rutasClient.get().uri("/api/dispatches/active")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    @CircuitBreaker(name = "rutas-service", fallbackMethod = "getAllDispatchesFallback")
    public List<Map<String, Object>> getAllDispatches() {
        return rutasClient.get().uri("/api/dispatches")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> getVehiclesFallback(Throwable t) { return Collections.emptyList(); }
    public List<Map<String, Object>> getActiveLoadsFallback(Throwable t) { return Collections.emptyList(); }
    public List<Map<String, Object>> getActiveDispatchesFallback(Throwable t) { return Collections.emptyList(); }
    public List<Map<String, Object>> getAllDispatchesFallback(Throwable t) { return Collections.emptyList(); }
}
