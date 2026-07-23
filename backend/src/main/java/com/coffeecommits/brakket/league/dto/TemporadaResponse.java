package com.coffeecommits.brakket.league.dto;

import com.coffeecommits.brakket.league.model.Temporada;
import java.time.LocalDate;

public record TemporadaResponse(
        Long id, Long ligaId, Long juegoId, String juegoNombre, String nombre,
        LocalDate fechaInicio, LocalDate fechaFin, String reglas, String estado,
        Integer cupoEquipos, Long formatoId, String formatoNombre, Long version,
        String mensaje
) {
    public static TemporadaResponse from(Temporada t, String mensaje) {
        return new TemporadaResponse(t.getId(), t.getLiga().getId(), t.getLiga().getJuego().getId(),
                t.getLiga().getJuego().getNombre(), t.getNombre(), t.getFechaInicio(), t.getFechaFin(),
                t.getReglas(), t.getEstado(), t.getCupoEquipos(),
                t.getFormato() == null ? null : t.getFormato().getId(),
                t.getFormato() == null ? "Sin formato (registro anterior)" : t.getFormato().getNombre(),
                t.getVersion(), mensaje);
    }
}
