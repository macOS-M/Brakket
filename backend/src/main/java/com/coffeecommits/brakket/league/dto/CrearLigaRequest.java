package com.coffeecommits.brakket.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear una liga (RF-22, EPIC-07).
 * El comisionado no viaja en el cuerpo: es el usuario autenticado.
 */
public record CrearLigaRequest(
        @NotBlank(message = "El nombre de la liga es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "Debe seleccionar un juego")
        Long juegoId
) {
}
