package com.coffeecommits.brakket.league.dto;

import com.coffeecommits.brakket.league.model.Liga;

/**
 * Representación de una liga que consume el frontend.
 * Aplana el juego y el comisionado para no exponer las entidades JPA.
 * Incluye el arte del juego para que las portadas puedan caer en él
 * cuando la liga no tiene foto propia.
 */
public record LigaResponse(
        Long id,
        String nombre,
        Long juegoId,
        String juegoNombre,
        String juegoImagenUrl,
        Long comisionadoId,
        String comisionadoNombre,
        String descripcion,
        String reglas,
        String fotoUrl
) {
    public static LigaResponse from(Liga liga) {
        return new LigaResponse(
                liga.getId(),
                liga.getNombre(),
                liga.getJuego().getId(),
                liga.getJuego().getNombre(),
                liga.getJuego().getImagenUrl(),
                liga.getComisionado().getId(),
                liga.getComisionado().getNombre(),
                liga.getDescripcion(),
                liga.getReglas(),
                liga.getFotoUrl()
        );
    }
}
