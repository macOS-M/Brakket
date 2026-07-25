package com.coffeecommits.brakket.twitch.dto;

import java.time.LocalDateTime;

/**
 * Indicadores básicos de audiencia de una transmisión (RF-36): muestras
 * capturadas, pico, promedio y duración. La consulta rica por períodos y
 * rangos horarios es RF-37 y queda fuera de este alcance.
 */
public record MetricasTransmisionResponse(
        Long transmisionId,
        String estado,
        long muestras,
        Integer pico,
        Double promedio,
        Long duracionMinutos,
        LocalDateTime iniciadaEn,
        LocalDateTime finalizadaEn,
        LocalDateTime ultimaMuestra) {}
