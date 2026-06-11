package com.malvinas.rutas.application.dto;

import java.time.LocalDateTime;

public record DispatchPointResponse(
    Long id, Long deliveryPointId, String deliveryPointName,
    Short visitOrder, String deliveryStatus, LocalDateTime deliveryTime, String remark
) {}
