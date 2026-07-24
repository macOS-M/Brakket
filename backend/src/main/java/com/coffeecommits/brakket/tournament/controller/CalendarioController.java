package com.coffeecommits.brakket.tournament.controller;

import com.coffeecommits.brakket.tournament.dto.CalendarioEventoResponse;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.service.CalendarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarioController {

    private final CalendarioService calendarioService;

    public CalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @GetMapping
    public List<CalendarioEventoResponse> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) Long juegoId,
            @RequestParam(required = false) Long ligaId,
            @RequestParam(required = false) Long torneoId,
            @RequestParam(required = false) EstadoTorneo estado,
            @RequestParam(required = false) Long equipoId,
            Authentication authentication) {
        return calendarioService.consultar(
                desde, hasta, juegoId, ligaId, torneoId, estado, equipoId,
                authentication.getName(), esAdmin(authentication));
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}