package com.coffeecommits.brakket.dispute.controller;

import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;
import com.coffeecommits.brakket.dispute.service.DisputaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments/partidas")
public class DisputaController {

    private final DisputaService disputaService;

    public DisputaController(DisputaService disputaService) {
        this.disputaService = disputaService;
    }

    /** RF-30: impugnar el resultado de una partida ya finalizada. */
    @PostMapping("/{partidaId}/disputas")
    @PreAuthorize("isAuthenticated()")
    public DisputaResponse impugnar(@PathVariable Long partidaId,
                                    @Valid @RequestBody ImpugnarResultadoRequest request,
                                    Authentication authentication) {
        return disputaService.impugnar(partidaId, authentication.getName(), esAdmin(authentication), request);
    }

    /** RF-31: para que el frontend sepa si hay una disputa activa y su ID. */
    @GetMapping("/{partidaId}/disputas")
    @PreAuthorize("isAuthenticated()")
    public java.util.List<DisputaResponse> listarPorPartida(@PathVariable Long partidaId,
                                                            Authentication authentication) {
        return disputaService.listarPorPartida(partidaId, authentication.getName(), esAdmin(authentication));
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}