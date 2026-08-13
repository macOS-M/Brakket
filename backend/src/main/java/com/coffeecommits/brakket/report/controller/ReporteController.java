package com.coffeecommits.brakket.report.controller;

import com.coffeecommits.brakket.report.dto.FiltrosReporteRequest;
import com.coffeecommits.brakket.report.dto.ReporteResponse;
import com.coffeecommits.brakket.report.model.TipoReporte;
import com.coffeecommits.brakket.report.service.ReportePdfService;
import com.coffeecommits.brakket.report.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;

    public ReporteController(ReporteService reporteService, ReportePdfService reportePdfService) {
        this.reporteService = reporteService;
        this.reportePdfService = reportePdfService;
    }

    /** RF-50: reporte para mostrar en pantalla. */
    @GetMapping
    @PreAuthorize("hasAuthority('EXPORTAR_REPORTES')")
    public ReporteResponse generar(@RequestParam String tipo,
                                   @RequestParam(required = false) Long torneoId,
                                   @RequestParam(required = false) Long patrocinadorId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   Authentication authentication) {
        FiltrosReporteRequest filtros = new FiltrosReporteRequest(torneoId, patrocinadorId, desde, hasta);
        return reporteService.generar(TipoReporte.desde(tipo), filtros, authentication);
    }

    /** RF-50: mismo reporte, empaquetado como PDF descargable. */
    @GetMapping("/pdf")
    @PreAuthorize("hasAuthority('EXPORTAR_REPORTES')")
    public ResponseEntity<byte[]> generarPdf(@RequestParam String tipo,
                                             @RequestParam(required = false) Long torneoId,
                                             @RequestParam(required = false) Long patrocinadorId,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             Authentication authentication) {
        TipoReporte tipoReporte = TipoReporte.desde(tipo);
        FiltrosReporteRequest filtros = new FiltrosReporteRequest(torneoId, patrocinadorId, desde, hasta);
        ReporteResponse reporte = reporteService.generar(tipoReporte, filtros, authentication);
        byte[] pdf = reportePdfService.generarPdf(reporte);

        String nombreArchivo = "reporte-" + tipoReporte.name().toLowerCase() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}