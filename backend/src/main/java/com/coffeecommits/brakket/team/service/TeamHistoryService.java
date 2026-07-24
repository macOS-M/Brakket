package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.HistorialEquipoResponse;

import java.time.LocalDate;

public interface TeamHistoryService {

    HistorialEquipoResponse obtenerHistorial(Long equipoId, String actorCorreo,
                                             LocalDate desde, LocalDate hasta);
}