package com.malvinas.rutas.infrastructure.controller;

/**
 * Centralizes the base REST path constants for the rutas-service.
 * Method-level sub-paths are declared as relative strings in each @GetMapping / @PostMapping.
 */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String DISPATCHES      = "/api/dispatches";
    public static final String DELIVERY_POINTS = "/api/delivery-points";
}
