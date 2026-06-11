package com.malvinas.personal.infrastructure.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " no encontrado con id: " + id);
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
