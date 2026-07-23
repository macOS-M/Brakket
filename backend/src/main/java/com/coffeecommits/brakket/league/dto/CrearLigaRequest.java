package com.coffeecommits.brakket.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear una liga (RF-22, EPIC-07).
 * El comisionado no viaja en el cuerpo: es el usuario autenticado.
 * Descripción, reglas y foto son opcionales; sin foto, el frontend usa
 * el arte del juego como portada.
 */
public record CrearLigaRequest(
        @NotBlank(message = "El nombre de la liga es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "Debe seleccionar un juego")
        Long juegoId,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,

        @Size(max = 4000, message = "Las reglas no pueden superar los 4000 caracteres")
        String reglas,

        @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
        String fotoUrl
) {
}
