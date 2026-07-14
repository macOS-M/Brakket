package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.service.TeamDissolutionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disolución de equipos (RF-03). Baja lógica: el equipo pasa a DISUELTO
 * conservando su historial. Requiere JWT; el service valida que el
 * solicitante sea el capitán activo del equipo.
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
        return teamDissolutionService.disolver(equipoId, request, authentication.getName());
    }
}
