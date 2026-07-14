package com.coffeecommits.brakket.common.exception;

/**
 * Acción denegada por falta de permisos sobre el recurso (se traduce a 403).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
