package com.malvinas.auth.application.dto;

public record LoginResponse(
    String accessToken, String refreshToken, String tokenType, long expiresIn,
    EmployeeInfo employee
) {
    public record EmployeeInfo(Long id, String dni, String fullName, String role) {}
}
