package com.coffeecommits.brakket.statistics.controller;

import com.coffeecommits.brakket.statistics.dto.CatalogoEstadisticasResponse;
import com.coffeecommits.brakket.statistics.dto.EstadisticasHistoricasResponse;
import com.coffeecommits.brakket.statistics.dto.PaginaOpcionesResponse;
import com.coffeecommits.brakket.statistics.service.EstadisticasHistoricasService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/statistics")
public class EstadisticasController {
    private final EstadisticasHistoricasService service;

    public EstadisticasController(EstadisticasHistoricasService service) { this.service = service; }

    @GetMapping("/catalog")
    public CatalogoEstadisticasResponse catalogo() { return service.catalogo(); }

    @GetMapping("/subjects")
    public PaginaOpcionesResponse buscarSujetos(@RequestParam String tipo,
                                                @RequestParam(defaultValue = "") String q,
                                                @RequestParam(required = false) Long juegoId,
                                                @RequestParam(defaultValue = "0") int pagina,
                                                @RequestParam(defaultValue = "5") int tamano) {
        return service.buscarSujetos(tipo, q, juegoId, pagina, tamano);
    }

    @GetMapping
    public EstadisticasHistoricasResponse consultar(
            @RequestParam(required = false) Long jugadorId,
            @RequestParam(required = false) Long equipoId,
            @RequestParam(required = false) Long juegoId,
            @RequestParam(required = false) Long temporadaId,
            @RequestParam(required = false) Long ligaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.consultar(jugadorId, equipoId, juegoId, temporadaId, ligaId, desde, hasta);
    }
}
