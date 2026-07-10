package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.AsignarRolRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;


public interface TeamRoleService {

    MiembroEquipoResponse cambiarRol(Long equipoId, Long miembroUsuarioId,
                                     AsignarRolRequest request, String solicitanteCorreo);
}