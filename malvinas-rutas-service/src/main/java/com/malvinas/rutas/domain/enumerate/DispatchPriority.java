package com.malvinas.rutas.domain.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DispatchPriority implements DisplayableEnum {
    HIGH  ("01", "Alta",  "Entrega urgente",                 true),
    MEDIUM("02", "Media", "Entrega en tiempo normal",        true),
    LOW   ("03", "Baja",  "Entrega con menor prioridad",     true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean available;
}
