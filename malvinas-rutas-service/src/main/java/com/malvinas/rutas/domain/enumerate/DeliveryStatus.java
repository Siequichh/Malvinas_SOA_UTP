package com.malvinas.rutas.domain.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryStatus implements DisplayableEnum {
    PENDING      ("01", "Pendiente",     "Punto de entrega pendiente",        true),
    DELIVERED    ("02", "Entregado",     "Entrega completada exitosamente",   true),
    PARTIAL      ("03", "Parcial",       "Entrega parcial",                   true),
    NOT_DELIVERED("04", "No Entregado",  "No se pudo realizar la entrega",    true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean available;
}
