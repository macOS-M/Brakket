package com.coffeecommits.brakket.common.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints básicos para verificar que la API responde y para conocer al usuario
 * autenticado. Sirven de humo/smoke test mientras se construyen los módulos.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /** Endpoint público de estado (no requiere autenticación). */
    @GetMapping("/public/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "app", "brakket-backend");
    }

    /**
     * Devuelve los datos del usuario autenticado. Con el modelo JWT, el principal
     * es el correo extraído del token por {@code JwtAuthenticationFilter}.
     */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Map.of("authenticated", false);
        }
        return Map.of(
                "authenticated", true,
                "correo", authentication.getName(),
                "roles", authentication.getAuthorities()
        );
    }
}
