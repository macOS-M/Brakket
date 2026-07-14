package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.CrearEquipoRequest;
import com.coffeecommits.brakket.team.dto.ActualizarEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.service.TeamRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
public class TeamRegistrationController {

    private final TeamRegistrationService teamRegistrationService;

    public TeamRegistrationController(TeamRegistrationService teamRegistrationService) {
        this.teamRegistrationService = teamRegistrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@Valid @RequestBody CrearEquipoRequest request,
                                Authentication authentication) {
        return teamRegistrationService.crear(request, authentication.getName());
    }

    @PutMapping("/{equipoId}")
    public EquipoResponse actualizar(@PathVariable Long equipoId,
                                     @Valid @RequestBody ActualizarEquipoRequest request,
                                     Authentication authentication) {
        return teamRegistrationService.actualizar(equipoId, request, authentication.getName());
    }
}
