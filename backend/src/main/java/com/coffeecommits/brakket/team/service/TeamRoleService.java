package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.AsignarRolRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;

import java.util.List;


public interface TeamRoleService {

    List<MiembroEquipoResponse> listarMiembros(Long equipoId);

    MiembroEquipoResponse cambiarRol(Long equipoId, Long miembroUsuarioId,
                                     AsignarRolRequest request, String solicitanteCorreo);
}