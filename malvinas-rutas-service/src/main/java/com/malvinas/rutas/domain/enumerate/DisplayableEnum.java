package com.malvinas.rutas.domain.enumerate;

public interface DisplayableEnum {
    String getCode();
    String getDisplayName();
    String getDescription();
    boolean isAvailable();

    static <E extends Enum<E> & DisplayableEnum> E fromCode(Class<E> enumClass, String code) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getCode().equals(code) && e.isAvailable()) return e;
        }
        throw new IllegalArgumentException("Codigo invalido para " + enumClass.getSimpleName() + ": " + code);
    }
}
