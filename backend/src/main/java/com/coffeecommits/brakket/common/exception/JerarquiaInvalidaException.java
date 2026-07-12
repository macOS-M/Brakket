package com.coffeecommits.brakket.common.exception;

/**
 * Se lanza cuando un usuario intenta una operación de roles por encima
 * de su jerarquía (criterio RF-19: "Un usuario no puede asignarse
 * permisos superiores a sí mismo"). El {@link GlobalExceptionHandler}
 * la traduce a HTTP 403.
 */
public class JerarquiaInvalidaException extends RuntimeException {

    public JerarquiaInvalidaException(String message) {
        super(message);
    }
}