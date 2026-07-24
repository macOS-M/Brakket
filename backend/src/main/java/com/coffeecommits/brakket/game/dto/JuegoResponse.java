package com.coffeecommits.brakket.game.dto;

import com.coffeecommits.brakket.game.model.Juego;

import java.time.LocalDate;
import java.util.List;

public record JuegoResponse(
        Long id,
        String nombre,
        String genero,
        String descripcion,
        String imagenUrl,
        Boolean activo,
        LocalDate fechaLanzamiento,
        Double rating,
        Integer metacritic,
        String plataformas,
        String etiquetas,
        String sitioWeb,
        List<String> capturas
) {

    public static JuegoResponse fromEntity(Juego juego) {
        return new JuegoResponse(
                juego.getId(),
                juego.getNombre(),
                juego.getGenero(),
                juego.getDescripcion(),
                juego.getImagenUrl(),
                juego.getActivo(),
                juego.getFechaLanzamiento(),
                juego.getRating(),
                juego.getMetacritic(),
                juego.getPlataformas(),
                juego.getEtiquetas(),
                juego.getSitioWeb(),
                juego.getCapturas() == null ? List.of() : juego.getCapturas()
        );
    }
}
