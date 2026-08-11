package com.coffeecommits.brakket.sponsorship.dto;

import com.coffeecommits.brakket.sponsorship.model.EspacioPublicitario;

import java.time.LocalDate;

public record EspacioPublicitarioResponse(
        Long id,
        Long patrocinioId,
        String patrocinadorNombre,
        String ubicacion,
        String imagenUrl,
        String enlaceUrl,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {

    public static EspacioPublicitarioResponse fromEntity(EspacioPublicitario e) {
        return new EspacioPublicitarioResponse(
                e.getId(),
                e.getPatrocinio().getId(),
                e.getPatrocinio().getPatrocinador().getNombre(),
                e.getUbicacion(),
                e.getImagenUrl(),
                e.getEnlaceUrl(),
                e.getEstado(),
                e.getPatrocinio().getFechaInicio(),
                e.getPatrocinio().getFechaFin()
        );
    }
}