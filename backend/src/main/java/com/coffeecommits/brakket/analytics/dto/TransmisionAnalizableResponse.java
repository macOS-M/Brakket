package com.coffeecommits.brakket.analytics.dto;

import java.time.LocalDateTime;

/**
 * RF-37: transmisión que el usuario puede consultar, ya filtrada por su rol.
 * Sin este catálogo no hay forma de obtener un id de transmisión: el listado de
 * RF-35 es público pero no lo expone, y el de RF-34 es solo de admin.
 */
public record TransmisionAnalizableResponse(
        Long id,
        String etiqueta,
        Long torneoId,
        String nombreTorneo,
        String estado,
        LocalDateTime iniciadaEn,
        LocalDateTime finalizadaEn,
        long muestras) {
}
