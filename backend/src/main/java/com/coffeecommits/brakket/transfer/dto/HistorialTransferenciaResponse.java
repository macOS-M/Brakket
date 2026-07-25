package com.coffeecommits.brakket.transfer.dto;

import com.coffeecommits.brakket.transfer.model.HistorialTransferencia;

import java.time.LocalDateTime;

public record HistorialTransferenciaResponse(
        Long id,
        Long jugadorId,
        String jugadorNombre,
        Long equipoOrigenId,
        String equipoOrigenNombre,
        Long equipoDestinoId,
        String equipoDestinoNombre,
        String rolAsignado,
        Long responsableId,
        String responsableNombre,
        LocalDateTime fechaTransferencia
) {

    public static HistorialTransferenciaResponse fromEntity(HistorialTransferencia h) {
        return new HistorialTransferenciaResponse(
                h.getId(),
                h.getJugador().getId(),
                h.getJugador().getNombre(),
                h.getEquipoOrigen().getId(),
                h.getEquipoOrigen().getNombre(),
                h.getEquipoDestino().getId(),
                h.getEquipoDestino().getNombre(),
                h.getRolAsignado(),
                h.getResponsable().getId(),
                h.getResponsable().getNombre(),
                h.getFechaTransferencia()
        );
    }
}