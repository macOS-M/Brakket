package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.ExpulsarIntegranteRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;

public interface TeamExpulsionService {

    /**
     * RF-10: expulsa a un integrante activo de la plantilla (baja logica).
     * Solo el capitan activo del equipo puede ejecutarla; la causa es
     * obligatoria y queda registrada junto con fecha y responsable.
     */
    MiembroEquipoResponse expulsar(Long equipoId, Long usuarioId,
                                   ExpulsarIntegranteRequest request, String solicitanteCorreo);
}
