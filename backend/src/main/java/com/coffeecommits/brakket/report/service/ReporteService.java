package com.coffeecommits.brakket.report.service;

import com.coffeecommits.brakket.report.dto.FiltrosReporteRequest;
import com.coffeecommits.brakket.report.dto.ReporteResponse;
import com.coffeecommits.brakket.report.model.TipoReporte;
import org.springframework.security.core.Authentication;

public interface ReporteService {

    /** RF-50: genera el reporte solicitado, ya aplanado para tabla, y registra la auditoría. */
    ReporteResponse generar(TipoReporte tipo, FiltrosReporteRequest filtros, Authentication authentication);
}