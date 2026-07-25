package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.Size;

/** Un jugador pide unirse a un equipo ajeno (mensaje opcional al capitán). */
public record SolicitarUnionRequest(
        @Size(max = 300, message = "El mensaje no puede superar los 300 caracteres")
        String mensaje
) {
}
