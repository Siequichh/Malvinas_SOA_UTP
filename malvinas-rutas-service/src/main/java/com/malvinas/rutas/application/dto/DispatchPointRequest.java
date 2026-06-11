package com.malvinas.rutas.application.dto;

import jakarta.validation.constraints.NotNull;

public record DispatchPointRequest(@NotNull Long deliveryPointId, Short visitOrder) {}
