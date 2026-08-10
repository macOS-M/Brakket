package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.tournament.dto.EventoTrazabilidadResponse;

import java.util.List;

public interface TrazabilidadService {

    /** RF-33: línea de tiempo completa de una partida, ordenada por fecha. */
    List<EventoTrazabilidadResponse> obtener(Long partidaId, String correo, boolean esAdmin);
}