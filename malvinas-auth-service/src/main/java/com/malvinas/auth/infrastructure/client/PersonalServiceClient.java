package com.malvinas.auth.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PersonalServiceClient {

    private final RestClient restClient;

    public PersonalServiceClient(@Qualifier("lbBuilder") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://personal-service").build();
    }

    /** null si el DNI no existe. Un 404 no cuenta como falla del circuit breaker. */
    @CircuitBreaker(name = "personal-service", fallbackMethod = "unavailable")
    public EmployeeAuthInfo findByDni(String dni) {
        return get("/api/employees/auth/by-dni/{key}", dni);
    }

    /** null si el ID no existe. Usado en el refresh para recuperar rol y nombre actuales. */
    @CircuitBreaker(name = "personal-service", fallbackMethod = "unavailable")
    public EmployeeAuthInfo findById(String employeeId) {
        return get("/api/employees/auth/by-id/{key}", employeeId);
    }

    private EmployeeAuthInfo get(String uri, String key) {
        try {
            return restClient.get().uri(uri, key).retrieve().body(EmployeeAuthInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    // Antes el fallback devolvia null y el usuario veia "credenciales invalidas" con personal caido.
    EmployeeAuthInfo unavailable(String key, Throwable t) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio de personal no disponible, intenta en un momento", t);
    }

    public record EmployeeAuthInfo(
        Long id, String dni, String firstName, String lastName,
        String passwordHash, String roleCode, Boolean isActive) {}
}
