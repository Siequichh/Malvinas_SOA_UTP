package com.malvinas.auth.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PersonalServiceClient {

    private final RestClient restClient;

    public PersonalServiceClient(@Qualifier("lbBuilder") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://personal-service").build();
    }

    @CircuitBreaker(name = "personal-service", fallbackMethod = "findByDniFallback")
    public EmployeeAuthInfo findByDni(String dni) {
        return restClient.get()
                .uri("/api/employees/auth/by-dni/{dni}", dni)
                .retrieve()
                .body(EmployeeAuthInfo.class);
    }

    @CircuitBreaker(name = "personal-service", fallbackMethod = "isActiveFallback")
    public boolean isEmployeeActive(String employeeId) {
        Map<String, Boolean> result = restClient.get()
                .uri("/api/employees/{id}/active", employeeId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result != null && Boolean.TRUE.equals(result.get("active"));
    }

    public EmployeeAuthInfo findByDniFallback(String dni, Throwable t) { return null; }
    public boolean isActiveFallback(String employeeId, Throwable t) { return false; }

    public record EmployeeAuthInfo(
        Long id, String dni, String firstName, String lastName,
        String passwordHash, String roleCode, Boolean isActive) {}
}
