package com.coffeecommits.brakket.report.dto;

import java.time.LocalDate;

/**
 * Filtros genéricos de RF-50 (torneo, período, patrocinador). Cada tipo de
 * reporte usa los que le aplican e ignora el resto — ver limitaciones
 * documentadas por tipo en ReporteServiceImpl.
 */
public record FiltrosReporteRequest(
        Long torneoId,
        Long patrocinadorId,
        LocalDate desde,
        LocalDate hasta
) {
}