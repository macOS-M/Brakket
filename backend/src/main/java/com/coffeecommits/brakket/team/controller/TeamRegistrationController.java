package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.CrearEquipoRequest;
import com.coffeecommits.brakket.team.dto.EditarEquipoRequest;
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

    @GetMapping("/{equipoId}")
    public EquipoResponse obtenerPorId(@PathVariable Long equipoId) {
        return teamRegistrationService.obtenerPorId(equipoId);
    }

    /**
     * RF-02: edición parcial de equipo. La autorización (solo el capitán
     * puede editar) se valida dentro del servicio, comparando el usuario
     * autenticado contra Equipo.capitan — no es un permiso global de
     * plataforma, sino sobre ese equipo puntual.
     */
    @PutMapping("/{equipoId}")
    public EquipoResponse editar(@PathVariable Long equipoId,
                                 @Valid @RequestBody EditarEquipoRequest request,
                                 Authentication authentication) {
        return teamRegistrationService.editar(equipoId, request, authentication.getName());
    }
}