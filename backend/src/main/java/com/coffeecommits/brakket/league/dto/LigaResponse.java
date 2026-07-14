package com.coffeecommits.brakket.league.dto;

import com.coffeecommits.brakket.league.model.Liga;

/**
 * Representación de una liga que consume el frontend.
 * Aplana el juego y el comisionado para no exponer las entidades JPA.
 */
public record LigaResponse(
        Long id,
        String nombre,
        Long juegoId,
        String juegoNombre,
        Long comisionadoId,
        String comisionadoNombre
) {
    public static LigaResponse from(Liga liga) {
        return new LigaResponse(
                liga.getId(),
                liga.getNombre(),
                liga.getJuego().getId(),
                liga.getJuego().getNombre(),
                liga.getComisionado().getId(),
                liga.getComisionado().getNombre()
        );
    }
}
