package com.coffeecommits.brakket.game.dto;

import java.time.LocalDate;
import java.util.List;

/** Ficha completa de un juego en RAWG (se pide una vez, al importar). */
public record JuegoExternoDetalle(
        String descripcion,
        LocalDate fechaLanzamiento,
        Double rating,
        Integer metacritic,
        String sitioWeb,
        List<String> plataformas,
        List<String> etiquetas,
        List<String> capturas
) {
}
