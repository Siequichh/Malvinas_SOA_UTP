package com.malvinas.rutas.infrastructure.util.converter;

import com.malvinas.rutas.domain.enumerate.DeliveryStatus;
import com.malvinas.rutas.domain.enumerate.DisplayableEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeliveryStatusConverter implements AttributeConverter<DeliveryStatus, String> {

    @Override
    public String convertToDatabaseColumn(DeliveryStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public DeliveryStatus convertToEntityAttribute(String code) {
        if (code == null) return null;
        return DisplayableEnum.fromCode(DeliveryStatus.class, code);
    }
}
