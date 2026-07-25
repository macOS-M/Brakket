package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.service.TeamDissolutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ciclo de vida del equipo (RF-03). Disolver es baja lógica (pasa a
 * DISUELTO conservando el historial); reactivar la revierte; eliminar es
 * borrado físico y solo procede sin historial competitivo. Requiere JWT;
 * el service valida capitanía o rol ADMIN (moderación).
 */
@RestController
@RequestMapping("/api/teams")
public class TeamDissolutionController {

    private final TeamDissolutionService teamDissolutionService;

    public TeamDissolutionController(TeamDissolutionService teamDissolutionService) {
        this.teamDissolutionService = teamDissolutionService;
    }

    @PatchMapping("/{equipoId}/disolver")
    public EquipoResponse disolver(@PathVariable Long equipoId,
                                   @Valid @RequestBody DisolverEquipoRequest request,
                                   Authentication authentication) {
        return teamDissolutionService.disolver(
                equipoId, request, authentication.getName(), esAdmin(authentication));
    }

    @PatchMapping("/{equipoId}/reactivar")
    public EquipoResponse reactivar(@PathVariable Long equipoId, Authentication authentication) {
        return teamDissolutionService.reactivar(
                equipoId, authentication.getName(), esAdmin(authentication));
    }

    @DeleteMapping("/{equipoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long equipoId, Authentication authentication) {
        teamDissolutionService.eliminar(
                equipoId, authentication.getName(), esAdmin(authentication));
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
