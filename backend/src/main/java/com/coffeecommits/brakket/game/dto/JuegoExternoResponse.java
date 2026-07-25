package com.coffeecommits.brakket.game.dto;

/**
 * Resultado del buscador externo de juegos (RAWG). Trae lo justo para
 * precargar el formulario del catálogo: nombre, género y arte oficial.
 */
public record JuegoExternoResponse(
        /** Slug de RAWG: la llave para pedir la ficha completa al importar. */
        String slug,
        String nombre,
        String genero,
        String imagenUrl
) {
}
