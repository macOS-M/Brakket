package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.dispute.dto.ApelarRequest;
import com.coffeecommits.brakket.dispute.dto.ApelacionResponse;
import com.coffeecommits.brakket.dispute.dto.ResolverApelacionRequest;

public interface ApelacionService {

    /** Solo dentro del plazo, sobre una disputa ya resuelta. */
    ApelacionResponse apelar(Long disputaId, String correo, boolean esAdmin, ApelarRequest request);

    /** Solo el comisionado de la liga (o admin); el árbitro no resuelve su propia apelación. */
    ApelacionResponse resolver(Long apelacionId, String correo, boolean esAdmin,
                               ResolverApelacionRequest request);

    java.util.List<ApelacionResponse> listarPorDisputa(Long disputaId);
}