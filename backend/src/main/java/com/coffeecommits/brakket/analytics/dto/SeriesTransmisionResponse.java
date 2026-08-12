package com.coffeecommits.brakket.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-37: series de métricas de una transmisión acotadas por rango y agrupadas.
 *
 * <p>La lista {@code series} trae siempre las cuatro claves de {@link ClaveSerie}.
 * Una serie sin datos viaja con {@code puntos} vacío y {@code muestras} en 0, nunca
 * ausente: así el frontend no cambia de forma el día que RF-39 empiece a escribir
 * sentimiento.
 */
public record SeriesTransmisionResponse(
        Long transmisionId,
        String etiquetaTransmision,
        String estado,
        String agrupacion,
        LocalDateTime desde,
        LocalDateTime hasta,
        Long duracionMinutos,
        Integer intervaloSegundos,
        String origen,
        Resumen resumen,
        List<Serie> series) {

    public record Serie(
            ClaveSerie clave,
            String etiqueta,
            String unidad,
            long muestras,
            Double promedio,
            Double pico,
            Double minimo,
            List<Punto> puntos) {
    }

    /** {@code valor} nulo es un hueco de muestreo, no un cero. */
    public record Punto(LocalDateTime instante, Double valor) {
    }

    public record Resumen(
            long muestrasAudiencia,
            Integer picoEspectadores,
            Double promedioEspectadores,
            long muestrasChat,
            Double promedioMensajesPorMinuto,
            Integer picoUsuariosActivos,
            long muestrasSentimiento,
            Double promedioPuntaje,
            String clasificacionPredominante) {
    }
}
