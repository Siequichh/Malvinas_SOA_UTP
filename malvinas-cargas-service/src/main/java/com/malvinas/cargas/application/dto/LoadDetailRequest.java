package com.malvinas.cargas.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record LoadDetailRequest(
    @NotBlank String description,
    Integer quantity,
    BigDecimal weightKg,
    String remark
) {}
