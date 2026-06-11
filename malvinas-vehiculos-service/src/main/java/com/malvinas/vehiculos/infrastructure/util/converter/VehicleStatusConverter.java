package com.malvinas.vehiculos.infrastructure.util.converter;

import com.malvinas.vehiculos.domain.enumerate.DisplayableEnum;
import com.malvinas.vehiculos.domain.enumerate.VehicleStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VehicleStatusConverter implements AttributeConverter<VehicleStatus, String> {
    @Override
    public String convertToDatabaseColumn(VehicleStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public VehicleStatus convertToEntityAttribute(String code) {
        return code == null ? null : DisplayableEnum.fromCode(VehicleStatus.class, code);
    }
}
