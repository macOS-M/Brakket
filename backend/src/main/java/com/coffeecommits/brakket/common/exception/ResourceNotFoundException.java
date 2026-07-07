package com.coffeecommits.brakket.common.exception;

/**
 * Se lanza cuando una entidad solicitada no existe.
 * El {@link GlobalExceptionHandler} la traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entidad, Object id) {
        super("%s no encontrado(a) con id %s".formatted(entidad, id));
    }
}
