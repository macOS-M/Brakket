package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.Equipo;

import java.util.List;

public record EquipoResponse(
        Long id,
        String nombre,
        String logo,
        String descripcion,
        Long juegoId,
        String juegoNombre,
        Long capitanId,
        String capitanNombre,
        List<String> redesSociales
) {

    public static EquipoResponse fromEntity(Equipo equipo, List<String> redesSociales) {
        return new EquipoResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getLogo(),
                equipo.getDescripcion(),
                equipo.getJuego() != null ? equipo.getJuego().getId() : null,
                equipo.getJuego() != null ? equipo.getJuego().getNombre() : null,
                equipo.getCapitan().getId(),
                equipo.getCapitan().getNombre(),
                redesSociales
        );
    }
}