package com.malvinas.rutas.infrastructure.mapper;

import com.malvinas.rutas.application.dto.DeliveryPointResponse;
import com.malvinas.rutas.domain.entity.DeliveryPoint;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeliveryPointMapper {

    DeliveryPointResponse toDto(DeliveryPoint deliveryPoint);
}
