package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.tournament.dto.CalendarioEventoResponse;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarioService {

    List<CalendarioEventoResponse> consultar(LocalDateTime desde, LocalDateTime hasta,
                                             Long juegoId, Long ligaId, Long torneoId,
                                             EstadoTorneo estado, Long equipoId,
                                             String correoSolicitante, boolean esAdmin);
}