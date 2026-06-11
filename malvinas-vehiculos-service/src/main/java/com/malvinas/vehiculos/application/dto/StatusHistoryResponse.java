package com.malvinas.vehiculos.application.dto;

import java.time.LocalDateTime;

public record StatusHistoryResponse(
    Long id, String previousStatusCode, String previousStatusDisplay,
    String newStatusCode, String newStatusDisplay,
    String reason, Long employeeId, LocalDateTime createdAt
) {}
