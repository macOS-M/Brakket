package com.coffeecommits.brakket.report.dto;

import com.coffeecommits.brakket.report.model.TipoReporte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoltorio común a los 4 tipos: título, filtros aplicados (texto legible,
 * para auditoría/lectura, criterio explícito del ERS), fecha y usuario
 * solicitante, y las filas de datos ya aplanadas para pintar como tabla
 * tanto en pantalla como en el PDF.
 */
public record ReporteResponse(
        TipoReporte tipo,
        String titulo,
        LocalDateTime fechaGeneracion,
        String usuarioSolicitante,
        String filtrosDescripcion,
        List<String> columnas,
        List<List<String>> filas
) {
}