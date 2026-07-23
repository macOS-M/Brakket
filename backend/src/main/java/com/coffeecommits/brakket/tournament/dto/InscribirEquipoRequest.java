package com.coffeecommits.brakket.tournament.dto;

import jakarta.validation.constraints.NotNull;

/** El capitán inscribe uno de sus equipos al torneo (RF-25). */
public record InscribirEquipoRequest(
        @NotNull(message = "Indicá el equipo a inscribir")
        Long equipoId
) {
}
