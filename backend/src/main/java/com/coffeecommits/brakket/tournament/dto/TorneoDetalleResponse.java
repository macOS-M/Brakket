package com.coffeecommits.brakket.tournament.dto;

import java.util.List;

/**
 * Detalle del torneo: los datos de la tarjeta, los equipos inscritos, y
 * los IDs de usuario de los árbitros asignados a este torneo (RF-28).
 * El frontend usa arbitrosIds para saber si el usuario autenticado puede
 * actuar como árbitro sobre las partidas de este torneo específico.
 */
public record TorneoDetalleResponse(
        TorneoResponse torneo,
        List<EquipoInscritoResponse> equipos,
        List<Long> arbitrosIds
) {
}