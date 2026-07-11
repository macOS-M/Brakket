package com.coffeecommits.brakket.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JuegoRequest(

        @NotBlank(message = "El nombre del juego es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @NotBlank(message = "El genero es obligatorio")
        @Size(max = 80, message = "El genero no puede superar los 80 caracteres")
        String genero,

        @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
        String descripcion
) {
}