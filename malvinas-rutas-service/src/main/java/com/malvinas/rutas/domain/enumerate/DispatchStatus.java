package com.malvinas.rutas.domain.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DispatchStatus implements DisplayableEnum {
    SCHEDULED("01", "Programado",  "Despacho programado, en espera de salida",       true),
    ON_ROUTE ("02", "En Ruta",     "Vehiculo en ruta de entrega",                    true),
    COMPLETED("03", "Completado",  "Despacho completado, vehiculo retorno a base",   true),
    CANCELLED("04", "Cancelado",   "Despacho cancelado",                             true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean available;
}
