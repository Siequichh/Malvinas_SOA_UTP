package com.malvinas.personal.domain.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus implements DisplayableEnum {
    PRESENT("01", "Presente", "Empleado presente y puntual", true),
    ABSENT("02", "Ausente", "Empleado ausente sin justificacion", true),
    LATE("03", "Tardanza", "Empleado llego tarde", true),
    JUSTIFIED("04", "Justificado", "Ausencia justificada", true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean available;
}
