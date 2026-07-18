package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.InvitacionEquipo;

import java.time.LocalDateTime;

public record InvitacionResponse(
        Long id,
        Long equipoId,
        String equipoNombre,
        Long jugadorId,
        String jugadorNombre,
        String rolPropuesto,
        String mensaje,
        String estado,
        Long creadoPorId,
        String creadoPorNombre,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaRespuesta
) {

    public static InvitacionResponse fromEntity(InvitacionEquipo inv) {
        return new InvitacionResponse(
                inv.getId(),
                inv.getEquipo().getId(),
                inv.getEquipo().getNombre(),
                inv.getJugador().getId(),
                inv.getJugador().getNombre(),
                inv.getRolPropuesto(),
                inv.getMensaje(),
                inv.getEstado(),
                inv.getCreadoPor().getId(),
                inv.getCreadoPor().getNombre(),
                inv.getFechaCreacion(),
                inv.getFechaRespuesta()
        );
    }
}