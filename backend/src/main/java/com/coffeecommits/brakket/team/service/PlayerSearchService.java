package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.JugadorDisponibleResponse;

import java.util.List;

public interface PlayerSearchService {

    List<JugadorDisponibleResponse> buscar(Long equipoId, String texto, Long juegoId,
                                           boolean soloDisponibles, String capitanCorreo);
}