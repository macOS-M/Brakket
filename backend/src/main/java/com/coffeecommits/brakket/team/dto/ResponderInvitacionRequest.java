package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.NotNull;

public record ResponderInvitacionRequest(

        @NotNull(message = "Debes indicar si aceptas o rechazas la invitacion")
        Boolean aceptar
) {
}