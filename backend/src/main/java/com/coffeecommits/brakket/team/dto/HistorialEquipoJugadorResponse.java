package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.MiembroEquipo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HistorialEquipoJugadorResponse(
        Long equipoId,
        String equipoNombre,
        Long juegoId,
        String juegoNombre,
        List<Long> juegoIds,
        List<String> juegoNombres,
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
                m.getEquipo().getJuegos().stream().map(j -> j.getId()).toList(),
                m.getEquipo().getJuegos().stream().map(j -> j.getNombre()).toList(),
                m.getRol(),
                m.getEstado(),
                m.getFechaUnion(),
                m.getFechaBaja()
        );
    }
}
