package com.coffeecommits.brakket.tournament.dto;

import com.coffeecommits.brakket.tournament.model.Torneo;

import java.time.LocalDateTime;

public record CalendarioEventoResponse(
        Long torneoId,
        String nombre,
        Long juegoId,
        String juegoNombre,
        Long ligaId,
        String ligaNombre,
        Long temporadaId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String estado,
        Boolean publico
) {

    public static CalendarioEventoResponse fromEntity(Torneo t) {
        var temporada = t.getTemporada();
        var liga = temporada != null ? temporada.getLiga() : null;
        return new CalendarioEventoResponse(
                t.getId(),
                t.getNombre(),
                t.getJuego().getId(),
                t.getJuego().getNombre(),
                liga != null ? liga.getId() : null,
                liga != null ? liga.getNombre() : null,
                temporada != null ? temporada.getId() : null,
                t.getFechaInicio(),
                t.getFechaFin(),
                t.getEstado().name(),
                t.getPublico()
        );
    }
}