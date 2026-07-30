package com.coffeecommits.brakket.dispute.dto;

import com.coffeecommits.brakket.dispute.model.Disputa;

import java.time.LocalDateTime;

public record DisputaResponse(
        Long id,
        Long partidaId,
        Long levantadaPorId,
        String levantadaPorNombre,
        String motivo,
        String descripcion,
        String evidenciaUrl,
        String estado,
        LocalDateTime fechaCreacion
) {

    // Arma la respuesta a partir de la entidad.
    public static DisputaResponse fromEntity(Disputa d) {
        return new DisputaResponse(
                d.getId(),
                d.getPartida().getId(),
                d.getLevantadaPor().getId(),
                d.getLevantadaPor().getNombre(),
                d.getMotivo(),
                d.getDescripcion(),
                d.getEvidenciaUrl(),
                d.getEstado(),
                d.getFechaCreacion()
        );
    }
}