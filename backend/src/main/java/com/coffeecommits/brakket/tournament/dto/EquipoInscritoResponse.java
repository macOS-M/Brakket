package com.coffeecommits.brakket.tournament.dto;

import java.util.List;

/** Equipo inscrito en un torneo, con su plantilla activa (RF-25). */
public record EquipoInscritoResponse(
        Long equipoId,
        String nombre,
        String logo,
        /** Gamertag del capitán dentro del juego (null en datos previos a V26). */
        String usuarioEnJuego,
        List<JugadorInscritoResponse> jugadores
) {
    public record JugadorInscritoResponse(Long usuarioId, String nombre, String rol) {
    }
}
