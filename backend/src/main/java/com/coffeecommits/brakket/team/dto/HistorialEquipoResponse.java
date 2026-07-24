package com.coffeecommits.brakket.team.dto;

import java.util.List;

/**
 * Respuesta completa del historial de un equipo (RF-16).
 */
public record HistorialEquipoResponse(
        Long equipoId,
        String equipoNombre,
        String equipoEstado,
        List<MovimientoPlantillaResponse> movimientos
) {
}