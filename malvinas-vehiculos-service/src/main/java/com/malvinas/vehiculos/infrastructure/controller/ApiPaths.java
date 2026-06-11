package com.malvinas.vehiculos.infrastructure.controller;

/**
 * Centralizes the base REST path constants for the vehiculos-service.
 * Method-level sub-paths are declared as relative strings in each @GetMapping / @PostMapping.
 */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String VEHICLES      = "/api/vehicles";
    public static final String VEHICLE_TYPES = "/api/vehicle-types";
}
