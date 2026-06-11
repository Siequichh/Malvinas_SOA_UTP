package com.malvinas.personal.application.dto.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AttendanceResponse(
    Integer id,
    Integer employeeId,
    String employeeName,
    LocalDate date,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    String status,
    String statusDisplay,
    String remark,
    LocalDateTime createdAt
) {}
