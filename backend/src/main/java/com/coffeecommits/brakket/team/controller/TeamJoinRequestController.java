package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.EquipoBusquedaResponse;
import com.coffeecommits.brakket.team.dto.ResponderSolicitudRequest;
import com.coffeecommits.brakket.team.dto.SolicitarUnionRequest;
import com.coffeecommits.brakket.team.dto.SolicitudUnionResponse;
import com.coffeecommits.brakket.team.service.TeamJoinRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solicitudes de unión a equipos + "mis equipos" del usuario autenticado. */
@RestController
public class TeamJoinRequestController {

    private final TeamJoinRequestService service;

    public TeamJoinRequestController(TeamJoinRequestService service) {
        this.service = service;
    }

    /** Equipos donde el usuario autenticado es miembro activo. */
    @GetMapping("/api/teams/mios")
    @PreAuthorize("isAuthenticated()")
    public List<EquipoBusquedaResponse> misEquipos(Authentication authentication) {
        return service.misEquipos(authentication.getName());
    }

    @PostMapping("/api/teams/{equipoId}/solicitudes")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudUnionResponse solicitar(@PathVariable Long equipoId,
                                            @Valid @RequestBody SolicitarUnionRequest request,
                                            Authentication authentication) {
        return service.solicitar(equipoId, authentication.getName(), request);
    }

    /** Pendientes del equipo; el service exige ser su capitán. */
    @GetMapping("/api/teams/{equipoId}/solicitudes")
    @PreAuthorize("isAuthenticated()")
    public List<SolicitudUnionResponse> pendientes(@PathVariable Long equipoId,
                                                   Authentication authentication) {
        return service.pendientesDeEquipo(equipoId, authentication.getName());
    }

    @PatchMapping("/api/solicitudes/{solicitudId}/responder")
    @PreAuthorize("isAuthenticated()")
    public SolicitudUnionResponse responder(@PathVariable Long solicitudId,
                                            @Valid @RequestBody ResponderSolicitudRequest request,
                                            Authentication authentication) {
        return service.responder(solicitudId, authentication.getName(), request.aceptar());
    }
}
