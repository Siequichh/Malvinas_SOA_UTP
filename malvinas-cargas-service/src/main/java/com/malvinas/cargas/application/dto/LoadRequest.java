package com.malvinas.cargas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LoadRequest(
    @NotBlank String vehiclePlate,
    @NotNull Long mobilizerId,
    String remarks,
    String loadingPlant,
    List<LoadDetailRequest> details
) {}
