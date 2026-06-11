package com.malvinas.auth.infrastructure.controller;

/**
 * Centralizes the base REST path constants for the auth-service.
 * Method-level sub-paths are declared as relative strings in each @PostMapping / @GetMapping.
 */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String AUTH = "/api/auth";
}
