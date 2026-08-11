package com.coffeecommits.brakket.analytics.model;

import java.math.BigDecimal;

/**
 * Clasificación cualitativa del sentimiento del chat (RF-39). Se persiste por
 * nombre en {@code analisis_sentimiento.clasificacion} y alimenta el termómetro
 * de RF-40 junto con el puntaje numérico.
 */
public enum ClasificacionSentimiento {
    POSITIVO,
    NEUTRO,
    NEGATIVO;

    /** A partir de |puntaje| ≥ este umbral el sentimiento deja de ser NEUTRO. */
    private static final BigDecimal UMBRAL = new BigDecimal("20");

    /**
     * Deriva la clasificación del puntaje.
     *
     * <p>Vive en el enum, y no dentro de cada analizador, para que el léxico y
     * la IA compartan exactamente la misma escala: la serie que dibuja RF-40
     * mezcla muestras de los dos motores, y un umbral distinto en cada uno haría
     * que el termómetro saltara de color al cambiar de proveedor sin que el chat
     * hubiera cambiado.</p>
     */
    public static ClasificacionSentimiento desdePuntaje(BigDecimal puntaje) {
        if (puntaje.compareTo(UMBRAL) >= 0) {
            return POSITIVO;
        }
        if (puntaje.compareTo(UMBRAL.negate()) <= 0) {
            return NEGATIVO;
        }
        return NEUTRO;
    }
}
