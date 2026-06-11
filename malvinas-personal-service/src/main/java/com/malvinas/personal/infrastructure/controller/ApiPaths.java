package com.malvinas.personal.infrastructure.controller;

/**
 * Centralizes the base REST path constants for the personal-service.
 * Method-level sub-paths are declared as relative strings in each @GetMapping / @PostMapping.
 */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String EMPLOYEES   = "/api/employees";
    public static final String ROLES       = "/api/roles";
    public static final String ATTENDANCES = "/api/attendances";
}
