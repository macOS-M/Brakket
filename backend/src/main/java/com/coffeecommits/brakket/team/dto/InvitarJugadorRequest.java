package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvitarJugadorRequest(

        @NotNull(message = "El jugador destinatario es obligatorio")
        Long jugadorId,

        @NotBlank(message = "El rol propuesto es obligatorio")
        String rolPropuesto,

        @Size(max = 300, message = "El mensaje no puede superar los 300 caracteres")
        String mensaje
) {
}