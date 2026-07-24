package com.coffeecommits.brakket.tournament.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Marcador de una partida (RF-29 mínimo). Lo usa tanto el reporte del
 * capitán como la resolución del organizador; en eliminación directa no
 * hay empates, así que los marcadores deben diferir (se valida en servicio).
 */
public record ReportarResultadoRequest(
        @NotNull(message = "Indicá el marcador del equipo A")
        @Min(value = 0, message = "El marcador no puede ser negativo")
        @Max(value = 999, message = "Marcador fuera de rango")
        Integer marcadorA,

        @NotNull(message = "Indicá el marcador del equipo B")
        @Min(value = 0, message = "El marcador no puede ser negativo")
        @Max(value = 999, message = "Marcador fuera de rango")
        Integer marcadorB
) {
}
