package com.coffeecommits.brakket.tournament.dto;

import com.coffeecommits.brakket.team.model.Equipo;

/** Equipo del capitán que puede inscribirse en el torneo consultado. */
public record EquipoElegibleResponse(Long id, String nombre, String logo) {

    public static EquipoElegibleResponse from(Equipo equipo) {
        return new EquipoElegibleResponse(equipo.getId(), equipo.getNombre(), equipo.getLogo());
    }
}
