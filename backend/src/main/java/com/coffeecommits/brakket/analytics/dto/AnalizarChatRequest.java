package com.coffeecommits.brakket.analytics.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Lote de mensajes de chat a analizar para una transmisión (RF-39).
 *
 * @param mensajes        mensajes de chat capturados (al menos uno no vacío)
 * @param usuariosActivos usuarios distintos que escribieron en la ventana; si
 *                        es null se aproxima con la cantidad de mensajes
 */
public record AnalizarChatRequest(
        @NotEmpty(message = "Debe enviar al menos un mensaje de chat para analizar")
        List<String> mensajes,

        Integer usuariosActivos
) {
}
