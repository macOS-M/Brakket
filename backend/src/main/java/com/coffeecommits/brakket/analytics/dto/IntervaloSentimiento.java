package com.coffeecommits.brakket.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un tramo de la evolución del sentimiento (RF-40).
 *
 * <p>La ERS pide la evolución "por intervalos", no muestra por muestra: en una
 * transmisión larga el muestreo deja una muestra por minuto y el gráfico crudo
 * sería ruido. Cada intervalo promedia las muestras que caen dentro.</p>
 *
 * <p>Solo se emiten intervalos con muestras: un hueco en la serie —el chat
 * mudo, o la captura caída— no debe dibujarse como un cero, que en esta escala
 * significa "chat neutro".</p>
 */
public record IntervaloSentimiento(
        LocalDateTime inicio,
        LocalDateTime fin,
        long muestras,
        BigDecimal puntajePromedio,
        String clasificacion
) {
}
