package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.AsignarRolRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;
import com.coffeecommits.brakket.team.service.TeamRoleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{equipoId}/miembros")
public class TeamRoleController {

    private final TeamRoleService teamRoleService;

    public TeamRoleController(TeamRoleService teamRoleService) {
        this.teamRoleService = teamRoleService;
    }

    @GetMapping
    public List<MiembroEquipoResponse> listar(@PathVariable Long equipoId) {
        return teamRoleService.listarMiembros(equipoId);
    }

    @PatchMapping("/{usuarioId}/rol")
    public MiembroEquipoResponse cambiarRol(@PathVariable Long equipoId,
                                            @PathVariable Long usuarioId,
                                            @Valid @RequestBody AsignarRolRequest request,
                                            Authentication authentication) {
        return teamRoleService.cambiarRol(equipoId, usuarioId, request, authentication.getName());
    }
}