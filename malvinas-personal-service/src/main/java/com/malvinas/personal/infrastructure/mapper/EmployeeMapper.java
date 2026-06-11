package com.malvinas.personal.infrastructure.mapper;

import com.malvinas.personal.application.dto.employee.EmployeeResponse;
import com.malvinas.personal.domain.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "fullName", expression = "java(employee.getFirstName() + \" \" + employee.getLastName())")
    @Mapping(source = "role.id", target = "roleId")
    @Mapping(source = "role.code", target = "roleCode")
    @Mapping(source = "role.name", target = "roleName")
    EmployeeResponse toDto(Employee employee);
}
