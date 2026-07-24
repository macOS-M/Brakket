package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.NotNull;

/** El capitán acepta o rechaza una solicitud de unión. */
public record ResponderSolicitudRequest(
        @NotNull(message = "Indicá si aceptás o rechazás la solicitud")
        Boolean aceptar
) {
}
