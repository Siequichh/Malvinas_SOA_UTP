package com.malvinas.rutas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record DispatchRequest(
    @NotBlank String vehiclePlate,
    @NotNull Long driverId,
    Long helper1Id,
    Long helper2Id,
    Long loadId,
    String priority,
    LocalTime scheduledDepartureTime,
    String remarks,
    List<DispatchPointRequest> points
) {}
