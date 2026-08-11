package com.coffeecommits.brakket.dispute.dto;

import com.coffeecommits.brakket.dispute.model.EvidenciaDisputa;

import java.time.LocalDateTime;

public record EvidenciaResponse(
        Long id,
        Long disputaId,
        Long subidoPorId,
        String subidoPorNombre,
        String url,
        String descripcion,
        LocalDateTime fechaCreacion
) {

    public static EvidenciaResponse fromEntity(EvidenciaDisputa e) {
        return new EvidenciaResponse(
                e.getId(),
                e.getDisputa().getId(),
                e.getSubidoPor().getId(),
                e.getSubidoPor().getNombre(),
                e.getUrl(),
                e.getDescripcion(),
                e.getFechaCreacion()
        );
    }
}