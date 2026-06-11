package com.malvinas.reportes.infrastructure.controller;

/**
 * Centralizes the base REST path constants for the reportes-service.
 * Method-level sub-paths are declared as relative strings in each @GetMapping.
 */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String REPORTS = "/api/reports";
    public static final String KPIS    = "/api/kpis";
}
