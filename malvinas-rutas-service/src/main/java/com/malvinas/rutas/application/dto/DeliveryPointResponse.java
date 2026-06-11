package com.malvinas.rutas.application.dto;

public record DeliveryPointResponse(
    Long id, String name, String address, String district,
    String reference, String contact, String phone, boolean isActive
) {}
