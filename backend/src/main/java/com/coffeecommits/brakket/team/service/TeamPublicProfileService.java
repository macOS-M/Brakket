package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.EquipoResumenPublicoResponse;
import com.coffeecommits.brakket.team.dto.PerfilEquipoPublicoResponse;

import java.util.List;

public interface TeamPublicProfileService {
    PerfilEquipoPublicoResponse consultarPerfil(Long equipoId, Long juegoId);

    /** Listado liviano: solo lo que la lista pública muestra. */
    List<EquipoResumenPublicoResponse> buscarEquipos(String criterio);
}
