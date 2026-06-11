package com.malvinas.personal.application.dto.role;

public record RoleResponse(
    Integer id,
    String code,
    String name,
    String description,
    Boolean isActive
) {}
