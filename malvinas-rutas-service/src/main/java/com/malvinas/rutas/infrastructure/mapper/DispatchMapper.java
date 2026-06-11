package com.malvinas.rutas.infrastructure.mapper;

import com.malvinas.rutas.application.dto.DispatchPointResponse;
import com.malvinas.rutas.application.dto.DispatchResponse;
import com.malvinas.rutas.domain.entity.Dispatch;
import com.malvinas.rutas.domain.entity.DispatchPoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DispatchMapper {

    @Mapping(target = "statusCode",     expression = "java(dispatch.getStatus().getCode())")
    @Mapping(target = "statusDisplay",  expression = "java(dispatch.getStatus().getDisplayName())")
    @Mapping(target = "priority",       expression = "java(dispatch.getPriority().getCode())")
    @Mapping(target = "priorityDisplay",expression = "java(dispatch.getPriority().getDisplayName())")
    @Mapping(target = "points", source = "dispatchPoints")
    DispatchResponse toDto(Dispatch dispatch);

    @Mapping(target = "deliveryPointId",   source = "deliveryPoint.id")
    @Mapping(target = "deliveryPointName", source = "deliveryPoint.name")
    @Mapping(target = "deliveryStatus",    expression = "java(point.getDeliveryStatus().getCode())")
    DispatchPointResponse toPointDto(DispatchPoint point);
}
