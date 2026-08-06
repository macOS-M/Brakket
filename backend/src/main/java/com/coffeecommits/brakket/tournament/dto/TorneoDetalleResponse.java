package com.coffeecommits.brakket.tournament.dto;

import java.util.List;

/**
 * Detalle del torneo: los datos de la tarjeta, los equipos inscritos, y
 * si el usuario autenticado es árbitro de este torneo (RF-28/RF-30). Se manda
 * ya calculado (no la lista completa de árbitros) para no exponer datos
 * de otros usuarios a cualquier visitante del torneo.
 */
public record TorneoDetalleResponse(
        TorneoResponse torneo,
        List<EquipoInscritoResponse> equipos,
        boolean esArbitro
) {
}