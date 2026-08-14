package com.coffeecommits.brakket.dispute.service;
import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;
import com.coffeecommits.brakket.dispute.dto.ResolverDisputaRequest;

import java.util.List;

public interface DisputaService {

    DisputaResponse impugnar(Long partidaId, String correo, boolean esAdmin,
                             ImpugnarResultadoRequest request);

    /** RF-31: para que el frontend sepa el ID de la disputa activa de una partida. */
    List<DisputaResponse> listarPorPartida(Long partidaId, String correo, boolean esAdmin);

    /** RF-32: solo árbitro del torneo, comisionado de su liga, o admin. */
    DisputaResponse resolver(Long disputaId, String correo, boolean esAdmin, ResolverDisputaRequest request);

    /** Vista panorámica: todas las disputas relevantes para el usuario, de cualquiera de sus torneos. */
    List<com.coffeecommits.brakket.dispute.dto.MiDisputaResponse> misDisputas(String correo, boolean esAdmin);
}