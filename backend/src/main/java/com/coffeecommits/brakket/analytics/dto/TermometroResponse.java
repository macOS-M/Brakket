package com.coffeecommits.brakket.analytics.dto;

import com.coffeecommits.brakket.analytics.model.EstadoTermometro;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Termómetro de sentimiento de una transmisión (RF-40).
 *
 * <p>Lectura completa para la vista: el estado, el indicador, su distribución,
 * la evolución por intervalos y un resumen en texto. Va aparte de
 * {@link SerieSentimientoResponse} —que sigue sirviendo al panel de RF-39— para
 * no cambiarle la forma a un contrato que ya tiene consumidor.</p>
 *
 * <p>Con estado distinto de {@code DISPONIBLE}, {@code puntajeGeneral} y
 * {@code clasificacion} viajan en null a propósito: la ERS pide ocultar el
 * termómetro definitivo cuando no hay datos suficientes, y mandar un número que
 * la vista no debe pintar es la forma más fácil de que termine pintado igual.</p>
 */
public record TermometroResponse(
        Long transmisionId,
        EstadoTermometro estado,

        /** Texto corto que explica el indicador; siempre presente. */
        String resumen,

        /** Período efectivamente consultado; null cuando no se acotó. */
        LocalDateTime desde,
        LocalDateTime hasta,
        int intervaloMinutos,

        /** Indicador general en [-100, 100]; null si el estado no es DISPONIBLE. */
        BigDecimal puntajeGeneral,
        String clasificacion,

        long totalMuestras,
        /** Muestras necesarias para considerar el indicador confiable. */
        int minimoMuestras,

        DistribucionSentimiento distribucion,
        List<IntervaloSentimiento> intervalos
) {
}
