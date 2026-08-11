package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.Equipo;

import java.time.LocalDateTime;
import java.util.List;

public record EquipoResponse(
        Long id,
        String nombre,
        String logo,
        String bannerUrl,
        String descripcion,
        String sitioWeb,
        String videoUrl,
        Long juegoId,
        String juegoNombre,
        List<Long> juegoIds,
        List<String> juegoNombres,
        Long capitanId,
        String capitanNombre,
        String estado,
        String estadoPrivacidad,
        Long version,
        List<String> redesSociales,
        LocalDateTime fechaDisolucion,
        String motivoDisolucion
) {

    public static EquipoResponse fromEntity(Equipo equipo) {
        return fromEntity(equipo, List.of());
    }

    public static EquipoResponse fromEntity(Equipo equipo, List<String> redesSociales) {
        return new EquipoResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getLogo(),
                equipo.getBannerUrl(),
                equipo.getDescripcion(),
                equipo.getSitioWeb(),
                equipo.getVideoUrl(),
                equipo.getJuego() != null ? equipo.getJuego().getId() : null,
                equipo.getJuego() != null ? equipo.getJuego().getNombre() : null,
                equipo.getJuegos().stream().map(j -> j.getId()).toList(),
                equipo.getJuegos().stream().map(j -> j.getNombre()).toList(),
                equipo.getCapitan().getId(),
                equipo.getCapitan().getNombre(),
                equipo.getEstado(),
                equipo.getEstadoPrivacidad(),
                equipo.getVersion(),
                redesSociales,
                equipo.getFechaDisolucion(),
                equipo.getMotivoDisolucion()
        );
    }
}
