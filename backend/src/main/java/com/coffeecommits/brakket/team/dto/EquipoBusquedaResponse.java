package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.team.model.Equipo;

/**
 * Resultado de la búsqueda de equipos (RF-05). Expone solo la información
 * pública del equipo: no incluye plantilla ni redes sociales.
 */
public record EquipoBusquedaResponse(
        Long id,
        String nombre,
        String logo,
        String bannerUrl,
        String descripcion,
        Long juegoId,
        String juegoNombre,
        String disciplina,
        String estado
) {

    public static EquipoBusquedaResponse fromEntity(Equipo equipo) {
        return new EquipoBusquedaResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getLogo(),
                equipo.getBannerUrl(),
                equipo.getDescripcion(),
                equipo.getJuego() != null ? equipo.getJuego().getId() : null,
                equipo.getJuego() != null ? equipo.getJuego().getNombre() : null,
                equipo.getJuego() != null ? equipo.getJuego().getGenero() : null,
                equipo.getEstado()
        );
    }
}
