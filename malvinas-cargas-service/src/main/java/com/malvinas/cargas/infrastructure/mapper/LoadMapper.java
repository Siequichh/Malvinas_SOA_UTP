package com.malvinas.cargas.infrastructure.mapper;

import com.malvinas.cargas.application.dto.LoadResponse;
import com.malvinas.cargas.domain.entity.Load;
import com.malvinas.cargas.domain.entity.LoadDetail;
import com.malvinas.cargas.domain.enumerate.LoadStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoadMapper {
    @Mapping(target = "statusCode", expression = "java(load.getStatus())")
    @Mapping(target = "statusDisplay", expression = "java(com.malvinas.cargas.domain.enumerate.LoadStatus.fromCode(load.getStatus()).getDisplayName())")
    LoadResponse toResponse(Load load);

    LoadResponse.LoadDetailResponse toDetailResponse(LoadDetail detail);
}
