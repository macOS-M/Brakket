package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;

/**
 * Disolución lógica de equipos (RF-03).
 */
public interface TeamDissolutionService {

    /**
     * Disuelve un equipo activo. Solo el capitán del equipo puede hacerlo y el
     * equipo no debe tener participación competitiva pendiente (inscripciones
     * activas o partidas sin resolver).
     */
    EquipoResponse disolver(Long equipoId, DisolverEquipoRequest request, String solicitanteCorreo);
}
