package com.coffeecommits.brakket.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Serie de sentimiento de una transmisión (RF-40): el último análisis, el
 * promedio del puntaje y todos los puntos, listo para el termómetro y su
 * tendencia.
 */
public record SerieSentimientoResponse(
        Long transmisionId,
        SentimientoResponse ultimo,
        BigDecimal promedioPuntaje,
        long totalMuestras,
        List<PuntoSentimiento> puntos
) {
}
