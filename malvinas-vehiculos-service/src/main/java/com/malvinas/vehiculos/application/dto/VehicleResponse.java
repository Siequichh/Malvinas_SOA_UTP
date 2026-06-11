package com.malvinas.vehiculos.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VehicleResponse(
    Long id, String licensePlate, String vehicleTypeName,
    String brand, String model, Short year, String color,
    String statusCode, String statusDisplay,
    Integer mileage, LocalDate soatExpiryDate, LocalDate technicalInspectionDate,
    boolean isActive, LocalDateTime createdAt, LocalDateTime modifiedAt
) {}
