package com.malvinas.cargas.infrastructure.controller;

/**
 * Centralizes the base REST path constants for the cargas-service.
 * Method-level sub-paths are declared as relative strings in each @GetMapping / @PostMapping.
 */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String LOADS = "/api/loads";
}
