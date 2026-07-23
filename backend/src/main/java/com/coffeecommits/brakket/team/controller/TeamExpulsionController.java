package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.ExpulsarIntegranteRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;
import com.coffeecommits.brakket.team.service.TeamExpulsionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams/{equipoId}/miembros")
public class TeamExpulsionController {

    private final TeamExpulsionService teamExpulsionService;

    public TeamExpulsionController(TeamExpulsionService teamExpulsionService) {
        this.teamExpulsionService = teamExpulsionService;
    }

    /** RF-10: expulsa a un integrante de la plantilla (baja logica con causa obligatoria). */
    @PatchMapping("/{usuarioId}/expulsar")
    public MiembroEquipoResponse expulsar(@PathVariable Long equipoId,
                                          @PathVariable Long usuarioId,
                                          @Valid @RequestBody ExpulsarIntegranteRequest request,
                                          Authentication authentication) {
        return teamExpulsionService.expulsar(equipoId, usuarioId, request, authentication.getName());
    }
}
