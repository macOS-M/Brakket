package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.HistorialEquipoJugadorResponse;

import java.time.LocalDate;
import java.util.List;

public interface PlayerHistoryService {

    List<HistorialEquipoJugadorResponse> historial(Long jugadorId, Long juegoId, LocalDate desde, LocalDate hasta,
                                                   String correoSolicitante, boolean esAdmin);
}