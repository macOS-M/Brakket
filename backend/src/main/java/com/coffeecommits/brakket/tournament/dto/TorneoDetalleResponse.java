package com.coffeecommits.brakket.tournament.dto;

import java.util.List;

/** Detalle del torneo: los datos de la tarjeta más los equipos inscritos. */
public record TorneoDetalleResponse(
        TorneoResponse torneo,
        List<EquipoInscritoResponse> equipos
) {
}
