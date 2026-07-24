package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.HistorialEquipoResponse;
import com.coffeecommits.brakket.team.service.TeamHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Consulta del historial de movimientos de plantilla de un equipo (RF-16).
 */
@RestController
@RequestMapping("/api/teams")
public class TeamHistoryController {

    private final TeamHistoryService teamHistoryService;

    public TeamHistoryController(TeamHistoryService teamHistoryService) {
        this.teamHistoryService = teamHistoryService;
    }

    /**
     * @param desde fecha inicial del rango, formato ISO (yyyy-MM-dd), opcional
     * @param hasta fecha final del rango, formato ISO (yyyy-MM-dd), opcional
     */
    @GetMapping("/{equipoId}/historial")
    public HistorialEquipoResponse historial(
            @PathVariable Long equipoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        return teamHistoryService.obtenerHistorial(equipoId, authentication.getName(), desde, hasta);
    }
}