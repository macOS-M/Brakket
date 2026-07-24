package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.MiembroEquipo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HistorialEquipoJugadorResponse(
        Long equipoId,
        String equipoNombre,
        Long juegoId,
        String juegoNombre,
        String rol,
        String estado,
        LocalDate fechaIngreso,
        LocalDateTime fechaSalida
) {

    public static HistorialEquipoJugadorResponse fromEntity(MiembroEquipo m) {
        var juego = m.getEquipo().getJuego();
        return new HistorialEquipoJugadorResponse(
                m.getEquipo().getId(),
                m.getEquipo().getNombre(),
                juego != null ? juego.getId() : null,
                juego != null ? juego.getNombre() : null,
                m.getRol(),
                m.getEstado(),
                m.getFechaUnion(),
                m.getFechaBaja()
        );
    }
}