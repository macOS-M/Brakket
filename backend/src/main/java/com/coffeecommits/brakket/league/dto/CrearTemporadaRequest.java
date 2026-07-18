package com.coffeecommits.brakket.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos de entrada para agregar una temporada a una liga (RF-22, EPIC-07).
 * La coherencia de fechas (fin posterior a inicio) la valida el servicio.
 */
public record CrearTemporadaRequest(
        @NotBlank(message = "El nombre de la temporada es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin
) {
}
