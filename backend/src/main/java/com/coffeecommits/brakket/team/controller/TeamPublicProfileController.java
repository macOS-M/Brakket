package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.EquipoResumenPublicoResponse;
import com.coffeecommits.brakket.team.dto.PerfilEquipoPublicoResponse;
import com.coffeecommits.brakket.team.service.TeamPublicProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/teams")
public class TeamPublicProfileController {
    private final TeamPublicProfileService profileService;

    public TeamPublicProfileController(TeamPublicProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<EquipoResumenPublicoResponse> buscar(@RequestParam(defaultValue = "") String criterio) {
        return profileService.buscarEquipos(criterio);
    }

    @GetMapping("/{equipoId}")
    public PerfilEquipoPublicoResponse consultar(@PathVariable Long equipoId,
                                                  @RequestParam(required = false) Long juegoId) {
        return profileService.consultarPerfil(equipoId, juegoId);
    }
}
