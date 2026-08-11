package com.coffeecommits.brakket.twitch.event;

import java.util.List;

/**
 * Se publicó una ventana de chat ya persistida como muestra (RF-38).
 *
 * <p>Lo emite el muestreo de chat después de guardar su {@code MetricaChat}, y
 * lo consume el análisis de sentimiento (RF-39). El evento existe para que el
 * muestreo no tenga que conocer al módulo de analytics: viaja el id de la
 * muestra, no la entidad, porque el consumidor corre en otra transacción.</p>
 *
 * <p>Los textos van en el evento pero <b>no se persisten</b>: quien los recibe
 * solo guarda el agregado, igual que hace RF-38 con el conteo.</p>
 *
 * @param metricaChatId muestra ya guardada a la que colgar el análisis
 * @param mensajes      textos capturados en la ventana
 */
public record MuestraChatCapturadaEvent(Long metricaChatId, List<String> mensajes) {
}
