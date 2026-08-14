package com.coffeecommits.brakket.dispute.controller;

import com.coffeecommits.brakket.dispute.service.TrazabilidadService;
import com.coffeecommits.brakket.tournament.dto.EventoTrazabilidadResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments/partidas")
public class TrazabilidadController {

    private final TrazabilidadService trazabilidadService;

    public TrazabilidadController(TrazabilidadService trazabilidadService) {
        this.trazabilidadService = trazabilidadService;
    }

    /** RF-33: línea de tiempo completa de una partida. */
    @GetMapping("/{partidaId}/trazabilidad")
    @PreAuthorize("isAuthenticated()")
    public List<EventoTrazabilidadResponse> obtener(@PathVariable Long partidaId, Authentication authentication) {
        return trazabilidadService.obtener(partidaId, authentication.getName(), esAdmin(authentication));
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}