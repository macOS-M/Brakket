package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.team.dto.JugadorDisponibleResponse;
import com.coffeecommits.brakket.team.service.PlayerSearchService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlayerSearchController {

    private final PlayerSearchService playerSearchService;

    public PlayerSearchController(PlayerSearchService playerSearchService) {
        this.playerSearchService = playerSearchService;
    }

    @GetMapping("/api/teams/{equipoId}/jugadores-disponibles")
    public PageResponse<JugadorDisponibleResponse> buscar(
            @PathVariable Long equipoId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long juegoId,
            @RequestParam(defaultValue = "false") boolean soloDisponibles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication) {
        return playerSearchService.buscar(
                equipoId, texto, juegoId, soloDisponibles, authentication.getName(), page, size);
    }
}
