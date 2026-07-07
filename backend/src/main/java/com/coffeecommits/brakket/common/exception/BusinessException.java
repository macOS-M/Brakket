package com.coffeecommits.brakket.common.exception;

/**
 * Error de regla de negocio (p. ej. cupo lleno, nombre duplicado, transición de
 * estado no permitida). El {@link GlobalExceptionHandler} la traduce a HTTP 409/400.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
