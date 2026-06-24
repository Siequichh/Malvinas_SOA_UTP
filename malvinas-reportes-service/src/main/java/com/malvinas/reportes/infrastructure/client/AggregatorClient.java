package com.malvinas.reportes.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AggregatorClient {

    private static final List<String> AUTH_HEADERS =
            List.of("X-Employee-Id", "X-Employee-Role", "X-Employee-Name");

    private final RestClient vehiculosClient;
    private final RestClient cargasClient;
    private final RestClient rutasClient;

    public AggregatorClient(
            @Value("${routes.vehiculos-uri:http://localhost:8082}") String vehiculosUri,
            @Value("${routes.cargas-uri:http://localhost:8083}") String cargasUri,
            @Value("${routes.rutas-uri:http://localhost:8084}") String rutasUri) {

        RestClient base = RestClient.builder().requestInterceptor(authInterceptor()).build();
        this.vehiculosClient = base.mutate().baseUrl(vehiculosUri).build();
        this.cargasClient    = base.mutate().baseUrl(cargasUri).build();
        this.rutasClient     = base.mutate().baseUrl(rutasUri).build();
    }

    // ponytail: propagate gateway-injected headers so downstream services can authenticate
    private ClientHttpRequestInterceptor authInterceptor() {
        return (request, body, execution) -> {
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest incoming = attrs.getRequest();
                    AUTH_HEADERS.forEach(h -> {
                        String v = incoming.getHeader(h);
                        if (v != null && !v.isBlank()) request.getHeaders().set(h, v);
                    });
                }
            } catch (Exception ignored) {}
            return execution.execute(request, body);
        };
    }

    @CircuitBreaker(name = "vehiculos-service", fallbackMethod = "getVehiclesFallback")
    public List<Map<String, Object>> getVehicles() {
        return vehiculosClient.get().uri("/api/vehicles")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @CircuitBreaker(name = "cargas-service", fallbackMethod = "getActiveLoadsFallback")
    public List<Map<String, Object>> getActiveLoads() {
        return cargasClient.get().uri("/api/loads/active")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @CircuitBreaker(name = "rutas-service", fallbackMethod = "getActiveDispatchesFallback")
    public List<Map<String, Object>> getActiveDispatches() {
        return rutasClient.get().uri("/api/dispatches/active")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @CircuitBreaker(name = "rutas-service", fallbackMethod = "getAllDispatchesFallback")
    public List<Map<String, Object>> getAllDispatches() {
        return rutasClient.get().uri("/api/dispatches")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<Map<String, Object>> getVehiclesFallback(Throwable t)         { return Collections.emptyList(); }
    public List<Map<String, Object>> getActiveLoadsFallback(Throwable t)       { return Collections.emptyList(); }
    public List<Map<String, Object>> getActiveDispatchesFallback(Throwable t)  { return Collections.emptyList(); }
    public List<Map<String, Object>> getAllDispatchesFallback(Throwable t)      { return Collections.emptyList(); }
}
