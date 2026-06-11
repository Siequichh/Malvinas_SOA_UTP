package com.malvinas.personal.application.dto.employee;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EmployeeRequest(
    @NotBlank(message = "DNI es requerido")
    @Size(min = 8, max = 8, message = "DNI debe tener 8 digitos")
    String dni,

    @NotBlank(message = "Nombre es requerido")
    @Size(max = 100)
    String firstName,

    @NotBlank(message = "Apellido es requerido")
    @Size(max = 100)
    String lastName,

    @Size(max = 15)
    String phone,

    @Email(message = "Email invalido")
    @Size(max = 150)
    String email,

    String password,

    @NotNull(message = "Rol es requerido")
    Integer roleId,

    LocalDate hireDate,
    String licenseNumber,
    String licenseCategory
) {}
