package com.coffeecommits.brakket.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint público básico para verificar que la API responde.
 * (El usuario autenticado se expone en {@code GET /api/me} del módulo auth.)
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /** Endpoint público de estado (no requiere autenticación). */
    @GetMapping("/public/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "app", "brakket-backend");
    }
}
