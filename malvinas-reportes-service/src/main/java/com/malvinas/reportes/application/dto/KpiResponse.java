package com.malvinas.reportes.application.dto;

public record KpiResponse(
    String name,
    String value,
    String unit,
    String trend
) {}
