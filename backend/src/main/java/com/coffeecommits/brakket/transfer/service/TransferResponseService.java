package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.transfer.dto.ResponderTransferenciaRequest;
import com.coffeecommits.brakket.transfer.dto.TransferenciaResponse;

import java.util.List;

/**
 * Respuesta a solicitudes de transferencia (RF-13, EPIC-03): el jugador y el
 * capitán del equipo origen aceptan o rechazan; con ambas aceptaciones la
 * transferencia se ejecuta, y un solo rechazo cancela el proceso completo.
 */
public interface TransferResponseService {

    /** Solicitudes pendientes donde el usuario es parte autorizada. */
    List<TransferenciaResponse> listarPendientes(String correo);

    /** Registra la respuesta de una parte autorizada y resuelve si corresponde. */
    TransferenciaResponse responder(String correo, Long solicitudId,
                                    ResponderTransferenciaRequest request);
}
