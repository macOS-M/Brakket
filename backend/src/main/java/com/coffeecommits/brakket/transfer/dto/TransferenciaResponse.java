package com.coffeecommits.brakket.transfer.dto;

import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;

import java.time.LocalDateTime;

public record TransferenciaResponse(
        Long id,
        Long jugadorId,
        String jugadorNombre,
        Long equipoOrigenId,
        String equipoOrigenNombre,
        Long equipoDestinoId,
        String equipoDestinoNombre,
        Long solicitanteId,
        String solicitanteNombre,
        String rolPropuesto,
        String justificacion,
        String estado,
        String aprobacionJugador,
        String aprobacionCapitanOrigen,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaResolucion
) {

    public static TransferenciaResponse fromEntity(SolicitudTransferencia s) {
        return new TransferenciaResponse(
                s.getId(),
                s.getJugador().getId(),
                s.getJugador().getNombre(),
                s.getEquipoOrigen().getId(),
                s.getEquipoOrigen().getNombre(),
                s.getEquipoDestino().getId(),
                s.getEquipoDestino().getNombre(),
                s.getSolicitante().getId(),
                s.getSolicitante().getNombre(),
                s.getRolPropuesto(),
                s.getJustificacion(),
                s.getEstado(),
                s.getAprobacionJugador(),
                s.getAprobacionCapitanOrigen(),
                s.getFechaSolicitud(),
                s.getFechaResolucion()
        );
    }
}
