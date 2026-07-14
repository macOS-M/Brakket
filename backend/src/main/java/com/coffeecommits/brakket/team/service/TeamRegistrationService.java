package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.CrearEquipoRequest;
import com.coffeecommits.brakket.team.dto.EditarEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;

public interface TeamRegistrationService {

    EquipoResponse crear(CrearEquipoRequest request, String creadorCorreo);

    EquipoResponse editar(Long equipoId, EditarEquipoRequest request, String actorCorreo);

    EquipoResponse obtenerPorId(Long equipoId);
}