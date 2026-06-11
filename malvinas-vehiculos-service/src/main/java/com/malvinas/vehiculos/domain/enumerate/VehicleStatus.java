package com.malvinas.vehiculos.domain.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum VehicleStatus implements DisplayableEnum {

    AVAILABLE("01", "Disponible", "Vehiculo listo para ser asignado.", true),
    LOADING("02", "En Carga", "Vehiculo en proceso de carga en planta.", true),
    LOADED("03", "Cargado", "Vehiculo cargado, listo para despacho.", true),
    ON_ROUTE("04", "En Ruta", "Vehiculo actualmente en ruta de entrega.", true),
    MAINTENANCE("05", "Mantenimiento", "Vehiculo en mantenimiento o revision tecnica.", true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean available;

    public List<VehicleStatus> getAllowedTransitions() {
        return switch (this) {
            case AVAILABLE   -> List.of(LOADING, MAINTENANCE);
            case LOADING     -> List.of(AVAILABLE, LOADED);
            case LOADED      -> List.of(ON_ROUTE, AVAILABLE);
            case ON_ROUTE, MAINTENANCE -> List.of(AVAILABLE);
        };
    }

    public boolean canTransitionTo(VehicleStatus target) {
        return getAllowedTransitions().contains(target);
    }
}
