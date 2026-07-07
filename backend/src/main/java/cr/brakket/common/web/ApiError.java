package cr.brakket.common.web;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo estándar de respuesta de error de la API.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public ApiError(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path, null);
    }
}
