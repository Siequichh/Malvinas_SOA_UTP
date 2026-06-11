package com.malvinas.rutas.application.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryPointRequest(
    @NotBlank String name, String address, String district,
    String reference, String contact, String phone
) {}
