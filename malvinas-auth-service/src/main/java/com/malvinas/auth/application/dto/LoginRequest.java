package com.malvinas.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(@NotBlank @Pattern(regexp = "\\d{8}", message = "DNI debe tener 8 digitos numericos") String dni, @NotBlank String password) {}
