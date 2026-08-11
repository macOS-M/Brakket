package com.coffeecommits.brakket.analytics.controller;

import com.coffeecommits.brakket.analytics.dto.AnalizarChatRequest;
import com.coffeecommits.brakket.analytics.dto.SentimientoResponse;
import com.coffeecommits.brakket.analytics.dto.SerieSentimientoResponse;
import com.coffeecommits.brakket.analytics.dto.TermometroResponse;
import com.coffeecommits.brakket.analytics.dto.TransmisionAnalizadaResponse;
import com.coffeecommits.brakket.analytics.service.SentimientoService;
import com.coffeecommits.brakket.analytics.service.TermometroService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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
    private final TermometroService termometroService;

    public SentimientoController(SentimientoService sentimientoService,
                                 TermometroService termometroService) {
        this.sentimientoService = sentimientoService;
        this.termometroService = termometroService;
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

    /**
     * Transmisiones con análisis disponible (RF-40), para el selector del
     * termómetro. Mismo permiso que el termómetro: quien no puede ver el
     * indicador tampoco necesita la lista.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VER_METRICAS_AUDIENCIA')")
    public List<TransmisionAnalizadaResponse> transmisionesAnalizadas() {
        return termometroService.transmisionesAnalizadas();
    }

    /**
     * Termómetro de sentimiento de la transmisión (RF-40): indicador,
     * distribución, evolución por intervalos y resumen.
     *
     * <p>Exige {@code VER_METRICAS_AUDIENCIA}, el permiso que RF-19 ya definió
     * como "consultar métricas de audiencia y sentimiento del chat" y que tienen
     * ADMIN, COMISIONADO y PATROCINADOR. Se pide el permiso y no una lista de
     * roles para que dar acceso a un rol nuevo siga siendo cuestión de datos.</p>
     */
    @GetMapping("/{id}/sentimiento/termometro")
    @PreAuthorize("hasAuthority('VER_METRICAS_AUDIENCIA')")
    public TermometroResponse termometro(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Integer intervaloMinutos) {
        return termometroService.consultar(id, desde, hasta, intervaloMinutos);
    }
}
