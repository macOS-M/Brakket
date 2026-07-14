package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.transfer.dto.CrearTransferenciaRequest;
import com.coffeecommits.brakket.transfer.dto.TransferenciaResponse;

import java.util.List;

/**
 * Solicitud de transferencia de jugadores entre equipos (RF-12, EPIC-03).
 */
public interface TransferRequestService {

    /**
     * Crea una solicitud de transferencia en estado PENDIENTE y notifica al
     * jugador y al capitán del equipo de origen. Solo puede iniciarla el
     * capitán del equipo destino.
     */
    TransferenciaResponse solicitar(String solicitanteCorreo, CrearTransferenciaRequest request);

    /** Seguimiento del equipo solicitante: solicitudes iniciadas por este capitán. */
    List<TransferenciaResponse> listarEnviadas(String solicitanteCorreo);
}
