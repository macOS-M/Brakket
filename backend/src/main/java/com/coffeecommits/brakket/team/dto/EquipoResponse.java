package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.Equipo;

import java.time.LocalDateTime;

public record EquipoResponse(
        Long id,
        String nombre,
        String logo,
        String descripcion,
        String estado,
        LocalDateTime fechaDisolucion,
        String motivoDisolucion
) {

    public static EquipoResponse fromEntity(Equipo equipo) {
        return new EquipoResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getLogo(),
                equipo.getDescripcion(),
                equipo.getEstado(),
                equipo.getFechaDisolucion(),
                equipo.getMotivoDisolucion()
        );
    }
}
