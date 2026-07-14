package com.coffeecommits.brakket.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para configurar/editar una liga existente (RF-22, EPIC-07).
 */
public record ActualizarLigaRequest(
        @NotBlank(message = "El nombre de la liga es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "Debe seleccionar un juego")
        Long juegoId
) {
}
