package com.coffeecommits.brakket.league.dto;

import com.coffeecommits.brakket.league.model.Temporada;

import java.time.LocalDate;

/**
 * Representación de una temporada que consume el frontend.
 */
public record TemporadaResponse(
        Long id,
        Long ligaId,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
    public static TemporadaResponse from(Temporada temporada) {
        return new TemporadaResponse(
                temporada.getId(),
                temporada.getLiga().getId(),
                temporada.getNombre(),
                temporada.getFechaInicio(),
                temporada.getFechaFin()
        );
    }
}
