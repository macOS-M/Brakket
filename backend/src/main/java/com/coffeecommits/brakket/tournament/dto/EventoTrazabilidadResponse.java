package com.coffeecommits.brakket.tournament.dto;

import java.time.LocalDateTime;

/** Un evento de la línea de tiempo de una partida (RF-33). */
public record EventoTrazabilidadResponse(
        String tipo,
        String descripcion,
        String autorNombre,
        LocalDateTime fecha
) {
}