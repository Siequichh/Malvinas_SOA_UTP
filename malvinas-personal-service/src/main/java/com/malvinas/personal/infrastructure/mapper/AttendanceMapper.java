package com.malvinas.personal.infrastructure.mapper;

import com.malvinas.personal.application.dto.attendance.AttendanceResponse;
import com.malvinas.personal.domain.entity.Attendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeName",
             expression = "java(attendance.getEmployee().getFirstName() + \" \" + attendance.getEmployee().getLastName())")
    @Mapping(target = "status",
             expression = "java(attendance.getStatus().getCode())")
    @Mapping(target = "statusDisplay",
             expression = "java(attendance.getStatus().getDisplayName())")
    AttendanceResponse toDto(Attendance attendance);
}
