package com.malvinas.vehiculos.infrastructure.mapper;

import com.malvinas.vehiculos.application.dto.VehicleResponse;
import com.malvinas.vehiculos.domain.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {
    @Mapping(target = "vehicleTypeName", source = "vehicleType.name")
    @Mapping(target = "statusCode", expression = "java(vehicle.getStatus().getCode())")
    @Mapping(target = "statusDisplay", expression = "java(vehicle.getStatus().getDisplayName())")
    @Mapping(source = "active", target = "isActive")
    VehicleResponse toResponse(Vehicle vehicle);
}
