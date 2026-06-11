package com.malvinas.vehiculos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusChangeRequest(@NotBlank String newStatusCode, String reason, Long employeeId) {}
