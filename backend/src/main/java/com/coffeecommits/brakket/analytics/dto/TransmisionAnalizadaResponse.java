package com.coffeecommits.brakket.analytics.dto;

import java.time.LocalDateTime;

/**
 * Transmisión con análisis de sentimiento disponible (RF-40).
 *
 * <p>Alimenta el selector del termómetro. Va en el módulo de analytics y no se
 * reusa el listado de RF-34 porque aquel es ADMIN y el termómetro también lo
 * consultan comisionados y patrocinadores; tampoco sirve el listado público de
 * RF-35, que describe canales y no expone el id de la transmisión.</p>
 *
 * <p>Solo aparecen transmisiones que ya tienen análisis: la ERS pide que el
 * indicador corresponda a una transmisión con análisis generado, y ofrecer en
 * la lista opciones que van a responder "pendiente" es hacer perder el viaje.</p>
 *
 * <p>Viaja el nombre del torneo además del id: el selector mostraba "torneo 27"
 * y no hay ninguna pantalla donde consultar a qué torneo corresponde ese número,
 * salvo abrirlo y leer la URL.</p>
 */
public record TransmisionAnalizadaResponse(
        Long id,
        String estado,
        LocalDateTime iniciadaEn,
        Long torneoId,
        String torneoNombre,
        Long totalMuestras
) {
}
