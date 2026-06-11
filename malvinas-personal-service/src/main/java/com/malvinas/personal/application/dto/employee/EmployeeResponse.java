package com.malvinas.personal.application.dto.employee;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
    Integer id,
    String dni,
    String firstName,
    String lastName,
    String fullName,
    String phone,
    String email,
    Integer roleId,
    String roleCode,
    String roleName,
    LocalDate hireDate,
    String licenseNumber,
    String licenseCategory,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {}
