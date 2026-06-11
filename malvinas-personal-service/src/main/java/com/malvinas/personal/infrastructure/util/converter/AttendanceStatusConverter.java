package com.malvinas.personal.infrastructure.util.converter;

import com.malvinas.personal.domain.enumerate.AttendanceStatus;
import com.malvinas.personal.domain.enumerate.DisplayableEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AttendanceStatusConverter implements AttributeConverter<AttendanceStatus, String> {

    @Override
    public String convertToDatabaseColumn(AttendanceStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public AttendanceStatus convertToEntityAttribute(String code) {
        if (code == null) return null;
        return DisplayableEnum.fromCode(AttendanceStatus.class, code);
    }
}
