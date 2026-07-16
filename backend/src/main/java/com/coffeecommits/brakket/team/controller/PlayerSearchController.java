package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.JugadorDisponibleResponse;
import com.coffeecommits.brakket.team.service.PlayerSearchService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PlayerSearchController {

    private final PlayerSearchService playerSearchService;

    public PlayerSearchController(PlayerSearchService playerSearchService) {
        this.playerSearchService = playerSearchService;
    }

    @GetMapping("/api/teams/{equipoId}/jugadores-disponibles")
    public List<JugadorDisponibleResponse> buscar(
            @PathVariable Long equipoId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long juegoId,
            @RequestParam(defaultValue = "false") boolean soloDisponibles,
            Authentication authentication) {
        return playerSearchService.buscar(equipoId, texto, juegoId, soloDisponibles, authentication.getName());
    }
}