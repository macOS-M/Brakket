package com.coffeecommits.brakket.dispute.service;
import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;
import com.coffeecommits.brakket.dispute.dto.ResolverDisputaRequest;

public interface DisputaService {

    DisputaResponse impugnar(Long partidaId, String correo, boolean esAdmin,
                             ImpugnarResultadoRequest request);

    /** RF-31: para que el frontend sepa el ID de la disputa activa de una partida. */
    java.util.List<DisputaResponse> listarPorPartida(Long partidaId, String correo, boolean esAdmin);

    /** RF-32: solo árbitro del torneo, comisionado de su liga, o admin. */
    DisputaResponse resolver(Long disputaId, String correo, boolean esAdmin, ResolverDisputaRequest request);
}