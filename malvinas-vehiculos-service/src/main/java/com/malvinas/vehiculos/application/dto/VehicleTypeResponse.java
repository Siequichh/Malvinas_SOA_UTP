package com.malvinas.vehiculos.application.dto;

import java.math.BigDecimal;

public record VehicleTypeResponse(Long id, String name, BigDecimal capacityKg, String description) {}
