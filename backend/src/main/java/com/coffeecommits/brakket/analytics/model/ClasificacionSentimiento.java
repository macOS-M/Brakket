package com.coffeecommits.brakket.analytics.model;

/**
 * Clasificación cualitativa del sentimiento del chat (RF-39). Se persiste por
 * nombre en {@code analisis_sentimiento.clasificacion} y alimenta el termómetro
 * de RF-40 junto con el puntaje numérico.
 */
public enum ClasificacionSentimiento {
    POSITIVO,
    NEUTRO,
    NEGATIVO
}
