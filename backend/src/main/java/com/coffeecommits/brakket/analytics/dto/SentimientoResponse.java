package com.coffeecommits.brakket.analytics.dto;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado de un análisis de sentimiento del chat (RF-39).
 */
public record SentimientoResponse(
        Long id,
        Long transmisionId,
        LocalDateTime fechaHora,
        String clasificacion,
        BigDecimal puntaje,
        int mensajesAnalizados,
        int usuariosActivos
) {
    public static SentimientoResponse from(AnalisisSentimiento analisis) {
        var metrica = analisis.getMetricaChat();
        Long transmisionId = metrica.getTransmisionTwitch() == null
                ? null : metrica.getTransmisionTwitch().getId();
        return new SentimientoResponse(
                analisis.getId(),
                transmisionId,
                analisis.getFechaHora(),
                analisis.getClasificacion(),
                analisis.getPuntaje(),
                metrica.getMensajesPorMinuto(),
                metrica.getUsuariosActivos()
        );
    }
}
