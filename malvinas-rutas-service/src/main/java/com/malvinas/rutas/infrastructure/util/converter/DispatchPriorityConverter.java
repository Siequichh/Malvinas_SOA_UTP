package com.malvinas.rutas.infrastructure.util.converter;

import com.malvinas.rutas.domain.enumerate.DispatchPriority;
import com.malvinas.rutas.domain.enumerate.DisplayableEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DispatchPriorityConverter implements AttributeConverter<DispatchPriority, String> {

    @Override
    public String convertToDatabaseColumn(DispatchPriority priority) {
        return priority == null ? null : priority.getCode();
    }

    @Override
    public DispatchPriority convertToEntityAttribute(String code) {
        if (code == null) return null;
        return DisplayableEnum.fromCode(DispatchPriority.class, code);
    }
}
