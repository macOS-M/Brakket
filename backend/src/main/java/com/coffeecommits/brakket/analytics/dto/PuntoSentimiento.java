package com.coffeecommits.brakket.analytics.dto;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Punto de la serie temporal de sentimiento (RF-40): fecha, clasificación y
 * puntaje de una muestra.
 */
public record PuntoSentimiento(
        LocalDateTime fechaHora,
        String clasificacion,
        BigDecimal puntaje
) {
    public static PuntoSentimiento from(AnalisisSentimiento analisis) {
        return new PuntoSentimiento(
                analisis.getFechaHora(),
                analisis.getClasificacion(),
                analisis.getPuntaje());
    }
}
