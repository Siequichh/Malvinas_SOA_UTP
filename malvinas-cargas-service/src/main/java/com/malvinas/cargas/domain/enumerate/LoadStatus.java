package com.malvinas.cargas.domain.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum LoadStatus implements DisplayableEnum {

    PENDING("01", "Pendiente", "Carga registrada, en espera de inicio.", true),
    IN_PROGRESS("02", "En Progreso", "Carga en proceso activo en planta.", true),
    COMPLETED("03", "Completado", "Carga finalizada correctamente.", true),
    CANCELLED("04", "Cancelado", "Carga cancelada antes de completarse.", true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean available;

    public static LoadStatus fromCode(String code) {
        return DisplayableEnum.fromCode(LoadStatus.class, code);
    }

    public List<LoadStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING     -> List.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> List.of(COMPLETED, CANCELLED);
            case COMPLETED   -> List.of();
            case CANCELLED   -> List.of();
        };
    }

    public boolean canTransitionTo(LoadStatus target) {
        return getAllowedTransitions().contains(target);
    }
}
