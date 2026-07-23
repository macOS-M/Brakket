package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.team.dto.JugadorDisponibleResponse;

public interface PlayerSearchService {

    PageResponse<JugadorDisponibleResponse> buscar(Long equipoId, String texto, Long juegoId,
                                                   boolean soloDisponibles, String capitanCorreo,
                                                   int page, int size);
}
