package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.PerfilEquipoPublicoResponse;

import java.util.List;

public interface TeamPublicProfileService {
    PerfilEquipoPublicoResponse consultarPerfil(Long equipoId, Long juegoId);

    List<PerfilEquipoPublicoResponse> buscarEquipos(String criterio);
}
