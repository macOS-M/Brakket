package com.coffeecommits.brakket.tournament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El capitán inscribe uno de sus equipos al torneo (RF-25). El usuario en
 * el juego (gamertag) identifica al equipo dentro de la lobby: sin API del
 * juego, es lo que hace calzar la cuenta de Brakket con quien juega.
 */
public record InscribirEquipoRequest(
        @NotNull(message = "Indicá el equipo a inscribir")
        Long equipoId,

        @NotBlank(message = "Indicá tu nombre de usuario dentro del juego")
        @Size(max = 100, message = "El usuario en el juego no puede superar los 100 caracteres")
        String usuarioEnJuego
) {
}
