package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;

public interface DisputaService {

    DisputaResponse impugnar(Long partidaId, String correo, boolean esAdmin,
                             ImpugnarResultadoRequest request);

    /** RF-31: para que el frontend sepa el ID de la disputa activa de una partida. */
    java.util.List<DisputaResponse> listarPorPartida(Long partidaId, String correo, boolean esAdmin);
}