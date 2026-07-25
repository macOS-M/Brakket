package com.coffeecommits.brakket.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Petición para traer un juego del catálogo externo (RAWG) al propio. */
public record ImportarJuegoRequest(

        @NotBlank(message = "Indicá el nombre del juego a importar")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre
) {
}
