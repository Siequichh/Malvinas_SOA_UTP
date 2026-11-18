package com.malvinas.cargas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record LoadDetailRequest(
    @NotBlank String description,
    @Positive(message = "La cantidad debe ser mayor a 0") Integer quantity,
    @Positive(message = "El peso debe ser mayor a 0") BigDecimal weightKg,
    String remark
) {}
