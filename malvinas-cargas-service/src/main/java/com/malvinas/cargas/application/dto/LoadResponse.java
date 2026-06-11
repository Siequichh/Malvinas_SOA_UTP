package com.malvinas.cargas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoadResponse(
    Long id, String vehiclePlate, Long mobilizerId,
    String statusCode, String statusDisplay,
    LocalDateTime loadingStartTime, LocalDateTime loadingEndTime,
    String remarks, String loadingPlant,
    List<LoadDetailResponse> details,
    LocalDateTime createdAt, LocalDateTime modifiedAt
) {
    public record LoadDetailResponse(
        Long id, String description, Integer quantity, BigDecimal weightKg, String remark
    ) {}
}
