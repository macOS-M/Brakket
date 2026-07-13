package com.coffeecommits.brakket.league.dto;

import com.coffeecommits.brakket.game.model.Juego;

/**
 * Opción de juego (id + nombre) para poblar el selector del formulario de liga.
 *
 * <p>Endpoint de solo lectura acotado a lo que necesita RF-22. El catálogo
 * completo de juegos (alta/edición/estado) es responsabilidad de RF-20.</p>
 */
public record JuegoOpcionResponse(
        Long id,
        String nombre
) {
    public static JuegoOpcionResponse from(Juego juego) {
        return new JuegoOpcionResponse(juego.getId(), juego.getNombre());
    }
}
