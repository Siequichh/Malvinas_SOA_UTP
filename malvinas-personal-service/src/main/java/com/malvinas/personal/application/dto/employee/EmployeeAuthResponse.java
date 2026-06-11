package com.malvinas.personal.application.dto.employee;

/**
 * Minimal employee projection returned exclusively to auth-service.
 * Contains the passwordHash required for credential verification.
 * Never exposed through the public employee endpoints.
 */
public record EmployeeAuthResponse(
        Integer id,
        String dni,
        String firstName,
        String lastName,
        String passwordHash,
        String roleCode,
        Boolean isActive
) {}
