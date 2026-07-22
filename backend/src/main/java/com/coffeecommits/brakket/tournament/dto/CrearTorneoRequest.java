package com.coffeecommits.brakket.tournament.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Datos para crear un torneo (RF-24, modelo abierto). El organizador no
 * viaja en el cuerpo: es el usuario autenticado. temporadaId es opcional:
 * sin ella el torneo es comunitario y cuelga directo del juego.
 */
public record CrearTorneoRequest(
        @NotBlank(message = "El nombre del torneo es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "Indicá el juego del torneo")
        Long juegoId,

        Long temporadaId,

        @NotBlank(message = "Indicá el formato del torneo")
        @Size(max = 60, message = "El formato no puede superar los 60 caracteres")
        String formato,

        @NotNull(message = "Indicá los jugadores por equipo")
        @Min(value = 1, message = "El mínimo es 1 jugador por lado")
        @Max(value = 10, message = "El máximo es 10 jugadores por lado")
        Integer tamanoEquipo,

        @NotNull(message = "Indicá el cupo de equipos")
        @Min(value = 2, message = "Un torneo necesita al menos 2 equipos")
        @Max(value = 64, message = "El cupo máximo es de 64 equipos")
        Integer maxEquipos,

        @NotNull(message = "Indicá la fecha y hora de inicio")
        LocalDateTime fechaInicio,

        Boolean publico,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion
) {
}
