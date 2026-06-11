package com.malvinas.vehiculos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record VehicleRequest(
    @NotBlank String licensePlate, @NotNull Long vehicleTypeId,
    String brand, String model, Short year, String color,
    Integer mileage, LocalDate soatExpiryDate, LocalDate technicalInspectionDate
) {}
