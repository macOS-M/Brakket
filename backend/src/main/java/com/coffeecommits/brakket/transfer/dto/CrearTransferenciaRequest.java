package com.coffeecommits.brakket.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada de RF-12: jugador, equipo de origen, equipo destino,
 * rol propuesto y justificación opcional.
 */
public record CrearTransferenciaRequest(
        @NotNull(message = "El jugador es obligatorio")
        Long jugadorId,

        @NotNull(message = "El equipo de origen es obligatorio")
        Long equipoOrigenId,

        @NotNull(message = "El equipo destino es obligatorio")
        Long equipoDestinoId,

        @NotNull(message = "El rol propuesto es obligatorio")
        @Size(max = 30, message = "El rol propuesto supera el largo máximo")
        String rolPropuesto,

        @Size(max = 500, message = "La justificación supera los 500 caracteres")
        String justificacion
) {
}
