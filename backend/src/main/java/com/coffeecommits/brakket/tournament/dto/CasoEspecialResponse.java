package com.coffeecommits.brakket.tournament.dto;

import com.coffeecommits.brakket.tournament.model.CasoEspecialPartida;

import java.time.LocalDateTime;

public record CasoEspecialResponse(
        Long id,
        Long partidaId,
        String tipo,
        String justificacion,
        String evidenciaUrl,
        Long registradoPorId,
        String registradoPorNombre,
        LocalDateTime fecha
) {

    public static CasoEspecialResponse fromEntity(CasoEspecialPartida c) {
        return new CasoEspecialResponse(
                c.getId(),
                c.getPartida().getId(),
                c.getTipo().name(),
                c.getJustificacion(),
                c.getEvidenciaUrl(),
                c.getRegistradoPor().getId(),
                c.getRegistradoPor().getNombre(),
                c.getFecha()
        );
    }
}