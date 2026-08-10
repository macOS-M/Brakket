package com.coffeecommits.brakket.analytics.dto;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado de un análisis de sentimiento del chat (RF-39).
 *
 * @param mensajesAnalizados cuántos mensajes entraron al motor. Solo se conoce
 *                           en el momento del análisis: los textos no se
 *                           persisten (solo el agregado), así que al
 *                           reconstruir el resultado desde la base va null
 * @param mensajesPorMinuto  tasa guardada en {@code metrica_chat}. Es una
 *                           <b>tasa</b>, no un conteo: el muestreo de RF-38
 *                           escribe la misma columna y hay que poder comparar
 */
public record SentimientoResponse(
        Long id,
        Long transmisionId,
        LocalDateTime fechaHora,
        String clasificacion,
        BigDecimal puntaje,
        Integer mensajesAnalizados,
        int mensajesPorMinuto,
        int usuariosActivos
) {
    /** Reconstruye el resultado desde la base (sin el conteo del lote original). */
    public static SentimientoResponse from(AnalisisSentimiento analisis) {
        return from(analisis, null);
    }

    public static SentimientoResponse from(AnalisisSentimiento analisis, Integer mensajesAnalizados) {
        var metrica = analisis.getMetricaChat();
        Long transmisionId = metrica.getTransmisionTwitch() == null
                ? null : metrica.getTransmisionTwitch().getId();
        return new SentimientoResponse(
                analisis.getId(),
                transmisionId,
                analisis.getFechaHora(),
                analisis.getClasificacion(),
                analisis.getPuntaje(),
                mensajesAnalizados,
                metrica.getMensajesPorMinuto(),
                metrica.getUsuariosActivos()
        );
    }
}
