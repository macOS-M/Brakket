package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.InvitacionResponse;
import com.coffeecommits.brakket.team.dto.InvitarJugadorRequest;
import com.coffeecommits.brakket.team.dto.ResponderInvitacionRequest;

import java.util.List;

public interface TeamInvitationService {

    InvitacionResponse invitar(Long equipoId, InvitarJugadorRequest request, String capitanCorreo);

    List<InvitacionResponse> misInvitacionesPendientes(String jugadorCorreo);

    InvitacionResponse responder(Long invitacionId, ResponderInvitacionRequest request, String jugadorCorreo);
}