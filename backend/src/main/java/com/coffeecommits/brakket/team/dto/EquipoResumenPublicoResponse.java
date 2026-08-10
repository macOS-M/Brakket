package com.coffeecommits.brakket.team.dto;

import java.util.List;

/**
 * Fila del listado público de equipos (RF-04). Solo lo que la lista muestra;
 * el perfil completo se consulta aparte por id.
 */
public record EquipoResumenPublicoResponse(
        Long id,
        String nombre,
        String logo,
        String juegoNombre,
        List<String> juegoNombres,
        long integrantesActivos
) {
}
