package com.coffeecommits.brakket.game.dto;

import com.coffeecommits.brakket.game.model.Juego;

public record JuegoResponse(
        Long id,
        String nombre,
        String genero,
        String descripcion,
        String imagenUrl,
        Boolean activo
) {

    public static JuegoResponse fromEntity(Juego juego) {
        return new JuegoResponse(
                juego.getId(),
                juego.getNombre(),
                juego.getGenero(),
                juego.getDescripcion(),
                juego.getImagenUrl(),
                juego.getActivo()
        );
    }
}