package com.coffeecommits.brakket.dispute.dto;

import com.coffeecommits.brakket.dispute.model.Disputa;

import java.time.LocalDateTime;

/**
 * Una fila de la vista panorámica "Mis disputas": junta el contexto del
 * torneo/partida (que DisputaResponse no trae, porque ese vive dentro
 * del contexto de una sola partida ya conocida) para poder listar
 * disputas de varios torneos a la vez.
 */
public record MiDisputaResponse(
        Long disputaId,
        Long torneoId,
        String torneoNombre,
        Long partidaId,
        String equipoANombre,
        String equipoBNombre,
        String motivo,
        String estado,
        String levantadaPorNombre,
        LocalDateTime fechaCreacion
) {
    public static MiDisputaResponse fromEntity(Disputa d) {
        var partida = d.getPartida();
        var torneo = partida.getTorneo();
        return new MiDisputaResponse(
                d.getId(),
                torneo.getId(),
                torneo.getNombre(),
                partida.getId(),
                partida.getEquipoA() != null ? partida.getEquipoA().getNombre() : null,
                partida.getEquipoB() != null ? partida.getEquipoB().getNombre() : null,
                d.getMotivo(),
                d.getEstado(),
                d.getLevantadaPor().getNombre(),
                d.getFechaCreacion()
        );
    }
}