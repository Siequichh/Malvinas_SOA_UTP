package com.malvinas.personal.application.dto.attendance;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceRequest(
    @NotNull(message = "ID de empleado requerido") Integer employeeId,
    LocalDate date,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    @NotNull(message = "Estado requerido") String status,
    String remark
) {}
