package com.malvinas.rutas.application.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DispatchResponse(
    Long id,
    String vehiclePlate,
    Long driverId,
    Long helper1Id,
    Long helper2Id,
    Long loadId,
    String priority,
    String priorityDisplay,
    String statusCode,
    String statusDisplay,
    LocalTime scheduledDepartureTime,
    LocalDateTime actualDepartureTime,
    LocalDateTime returnTime,
    String remarks,
    String loadingOrderCode,
    List<DispatchPointResponse> points,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {}
