package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.SolicitudUnion;

import java.time.LocalDateTime;

/** Solicitud de unión aplanada para el frontend. */
public record SolicitudUnionResponse(
        Long id,
        Long equipoId,
        String equipoNombre,
        Long jugadorId,
        String jugadorNombre,
        String mensaje,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaRespuesta
) {
    public static SolicitudUnionResponse fromEntity(SolicitudUnion s) {
        return new SolicitudUnionResponse(
                s.getId(),
                s.getEquipo().getId(),
                s.getEquipo().getNombre(),
                s.getJugador().getId(),
                s.getJugador().getNombre(),
                s.getMensaje(),
                s.getEstado(),
                s.getFechaCreacion(),
                s.getFechaRespuesta());
    }
}
