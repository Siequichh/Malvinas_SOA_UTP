package com.malvinas.personal.infrastructure.mapper;

import com.malvinas.personal.application.dto.role.RoleResponse;
import com.malvinas.personal.domain.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toDto(Role role);
}
