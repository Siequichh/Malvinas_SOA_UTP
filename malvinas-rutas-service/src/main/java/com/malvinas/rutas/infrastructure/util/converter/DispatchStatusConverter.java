package com.malvinas.rutas.infrastructure.util.converter;

import com.malvinas.rutas.domain.enumerate.DispatchStatus;
import com.malvinas.rutas.domain.enumerate.DisplayableEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DispatchStatusConverter implements AttributeConverter<DispatchStatus, String> {

    @Override
    public String convertToDatabaseColumn(DispatchStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public DispatchStatus convertToEntityAttribute(String code) {
        if (code == null) return null;
        return DisplayableEnum.fromCode(DispatchStatus.class, code);
    }
}
