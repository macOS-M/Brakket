package com.coffeecommits.brakket.team.dto;

import java.time.LocalDateTime;

/**
 * Una entrada en la línea de tiempo de movimientos de plantilla de un
 * equipo (RF-16): alta, baja o transferencia. Los campos no aplicables
 * a un tipo quedan en null (ej: causaBaja solo aplica a BAJA).
 */
public record MovimientoPlantillaResponse(
        String tipo,                    // ALTA / BAJA / TRANSFERENCIA
        LocalDateTime fecha,
        Long jugadorId,
        String jugadorNombre,
        Boolean jugadorPerfilPublico,    // para saber si se puede ver el detalle
        String rol,                      // rol asignado en ALTA/TRANSFERENCIA
        String causaBaja,                // solo BAJA
        Long equipoOrigenId,             // solo TRANSFERENCIA
        String equipoOrigenNombre,
        Long equipoDestinoId,            // solo TRANSFERENCIA
        String equipoDestinoNombre,
        Long responsableId,
        String responsableNombre
) {
}