package com.malvinas.vehiculos.infrastructure.mapper;

import com.malvinas.vehiculos.application.dto.VehicleTypeResponse;
import com.malvinas.vehiculos.domain.entity.VehicleType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleTypeMapper {
    VehicleTypeResponse toResponse(VehicleType vehicleType);
}
