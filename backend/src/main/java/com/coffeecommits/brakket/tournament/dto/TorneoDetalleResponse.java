package com.coffeecommits.brakket.tournament.dto;

import java.util.List;

/**
 * Detalle del torneo: los datos de la tarjeta, los equipos inscritos, y el rol
 * del usuario autenticado sobre este torneo (RF-28/RF-30/RF-32). Se mandan ya
 * calculados (no la lista de árbitros ni el comisionado de la liga) para no
 * exponer datos de otros usuarios a cualquier visitante del torneo.
 *
 * <p>{@code puedeResolverDisputa} y {@code puedeResolverApelacion} replican la
 * autorización del backend para que la interfaz muestre cada acción a quien
 * realmente puede ejecutarla: antes el botón se le ofrecía al organizador —
 * que el RF excluye a propósito — y nunca al comisionado, que es el único que
 * puede cerrar una apelación.</p>
 */
public record TorneoDetalleResponse(
        TorneoResponse torneo,
        List<EquipoInscritoResponse> equipos,
        boolean esArbitro,
        boolean esComisionado,
        boolean puedeResolverDisputa,
        boolean puedeResolverApelacion
) {
}