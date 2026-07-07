package com.coffeecommits.brakket.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Envoltura estándar de las respuestas de la API.
 *
 * <p>Éxito: {@code ApiResponse.ok(data)} → success=true, data con el contenido.
 * Error: lo construye el {@link com.coffeecommits.brakket.common.exception.GlobalExceptionHandler}.</p>
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Map<String, String> errors,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, errors, LocalDateTime.now());
    }
}
