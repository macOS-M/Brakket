package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;

public interface DisputaService {

    DisputaResponse impugnar(Long partidaId, String correo, boolean esAdmin,
                             ImpugnarResultadoRequest request);
}