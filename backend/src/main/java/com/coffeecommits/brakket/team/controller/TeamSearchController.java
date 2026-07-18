package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.team.dto.EquipoBusquedaResponse;
import com.coffeecommits.brakket.team.service.TeamSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de búsqueda de equipos (RF-05, EPIC-02). Requiere JWT válido
 * (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/teams")
public class TeamSearchController {

    private final TeamSearchService teamSearchService;

    public TeamSearchController(TeamSearchService teamSearchService) {
        this.teamSearchService = teamSearchService;
    }

    /**
     * Busca equipos por texto, juego, disciplina y/o estado; sin filtros
     * devuelve el listado completo paginado.
     */
    @GetMapping("/search")
    public PageResponse<EquipoBusquedaResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long juegoId,
            @RequestParam(required = false) String disciplina,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return teamSearchService.buscar(q, juegoId, disciplina, estado, page, size);
    }
}
