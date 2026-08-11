package com.coffeecommits.brakket.analytics.model;

/**
 * Estado del termómetro de sentimiento de una transmisión (RF-40).
 *
 * <p>La ERS distingue dos formas de "no hay indicador" y pide tratarlas
 * distinto: una transmisión que todavía no produjo análisis no es lo mismo que
 * una con tan pocas muestras que el indicador no sería confiable. La primera se
 * resuelve esperando; la segunda hay que avisarla.</p>
 */
public enum EstadoTermometro {

    /** Sin análisis en el período: el muestreo aún no produjo resultados. */
    PENDIENTE,

    /** Hay análisis, pero menos que el mínimo para mostrar un indicador. */
    INSUFICIENTE,

    /** Muestras suficientes: el termómetro se puede mostrar. */
    DISPONIBLE
}
