package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.HistorialEquipoJugadorResponse;
import com.coffeecommits.brakket.team.service.PlayerHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/players/{jugadorId}/historial-equipos")
public class PlayerHistoryController {

    private final PlayerHistoryService playerHistoryService;

    public PlayerHistoryController(PlayerHistoryService playerHistoryService) {
        this.playerHistoryService = playerHistoryService;
    }

    @GetMapping
    public List<HistorialEquipoJugadorResponse> historial(
            @PathVariable Long jugadorId,
            @RequestParam(required = false) Long juegoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        return playerHistoryService.historial(
                jugadorId, juegoId, desde, hasta, correoDe(authentication), esAdmin(authentication));
    }

    private static String correoDe(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}