package com.coffeecommits.brakket.team.controller;

import com.coffeecommits.brakket.team.dto.InvitacionResponse;
import com.coffeecommits.brakket.team.dto.InvitarJugadorRequest;
import com.coffeecommits.brakket.team.dto.ResponderInvitacionRequest;
import com.coffeecommits.brakket.team.service.TeamInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TeamInvitationController {

    private final TeamInvitationService teamInvitationService;

    public TeamInvitationController(TeamInvitationService teamInvitationService) {
        this.teamInvitationService = teamInvitationService;
    }

    @PostMapping("/api/teams/{equipoId}/invitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitacionResponse invitar(@PathVariable Long equipoId,
                                      @Valid @RequestBody InvitarJugadorRequest request,
                                      Authentication authentication) {
        return teamInvitationService.invitar(equipoId, request, authentication.getName());
    }

    @GetMapping("/api/invitaciones/pendientes")
    public List<InvitacionResponse> misPendientes(Authentication authentication) {
        return teamInvitationService.misInvitacionesPendientes(authentication.getName());
    }

    @PatchMapping("/api/invitaciones/{invitacionId}/responder")
    public InvitacionResponse responder(@PathVariable Long invitacionId,
                                        @Valid @RequestBody ResponderInvitacionRequest request,
                                        Authentication authentication) {
        return teamInvitationService.responder(invitacionId, request, authentication.getName());
    }
}