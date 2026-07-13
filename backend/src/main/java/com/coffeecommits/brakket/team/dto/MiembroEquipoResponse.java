package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.MiembroEquipo;

public record MiembroEquipoResponse(
        Long id,
        Long equipoId,
        Long usuarioId,
        String nombreUsuario,
        String rol,
        String estado
) {

    public static MiembroEquipoResponse fromEntity(MiembroEquipo miembro) {
        return new MiembroEquipoResponse(
                miembro.getId(),
                miembro.getEquipo().getId(),
                miembro.getUsuario().getId(),
                miembro.getUsuario().getNombre(),
                miembro.getRol(),
                miembro.getEstado()
        );
    }
}