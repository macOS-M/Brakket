package com.coffeecommits.brakket.analytics.controller;

import com.coffeecommits.brakket.analytics.dto.AnalizarChatRequest;
import com.coffeecommits.brakket.analytics.dto.SentimientoResponse;
import com.coffeecommits.brakket.analytics.dto.SerieSentimientoResponse;
import com.coffeecommits.brakket.analytics.service.SentimientoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de análisis de sentimiento del chat (RF-39, EPIC-10).
 *
 * <p>Ejecutar un análisis es una operación de administración de la integración
 * ({@code @PreAuthorize ADMIN}, igual que el resto del panel de Twitch); leer la
 * serie exige solo sesión, para que el termómetro (RF-40) pueda mostrarse en las
 * vistas de la plataforma.</p>
 */
@RestController
@RequestMapping("/api/analytics/transmisiones")
public class SentimientoController {

    private final SentimientoService sentimientoService;

    public SentimientoController(SentimientoService sentimientoService) {
        this.sentimientoService = sentimientoService;
    }

    /** Analiza un lote de mensajes de chat de la transmisión y lo persiste. */
    @PostMapping("/{id}/sentimiento")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SentimientoResponse analizar(@PathVariable Long id,
                                        @Valid @RequestBody AnalizarChatRequest request) {
        return sentimientoService.analizar(id, request);
    }

    /** Serie de sentimiento de la transmisión (para el termómetro de RF-40). */
    @GetMapping("/{id}/sentimiento")
    public SerieSentimientoResponse serie(@PathVariable Long id) {
        return sentimientoService.serie(id);
    }
}
