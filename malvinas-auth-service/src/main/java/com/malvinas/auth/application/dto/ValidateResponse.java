package com.malvinas.auth.application.dto;

public record ValidateResponse(boolean valid, String employeeId, String role, String name) {}
