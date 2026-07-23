package com.coffeecommits.brakket.league.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Configuracion de una temporada de liga (RF-23). */
public record CrearTemporadaRequest(
        @NotBlank(message = "El nombre de la temporada es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        @NotBlank(message = "Las reglas son obligatorias")
        @Size(max = 5000, message = "Las reglas no pueden superar los 5000 caracteres")
        String reglas,
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "PLANIFICADA|ACTIVA|FINALIZADA|CANCELADA", message = "El estado seleccionado no es valido")
        String estado,
        @NotNull(message = "El cupo es obligatorio")
        @Min(value = 2, message = "El cupo minimo es de 2 equipos")
        @Max(value = 1024, message = "El cupo maximo es de 1024 equipos")
        Integer cupoEquipos,
        @NotNull(message = "El formato es obligatorio") Long formatoId
) {
    /** Compatibilidad con clientes anteriores; el servicio exige un formato activo. */
    public CrearTemporadaRequest(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        this(nombre, fechaInicio, fechaFin, "Reglas generales", "PLANIFICADA", 2, null);
    }
}
