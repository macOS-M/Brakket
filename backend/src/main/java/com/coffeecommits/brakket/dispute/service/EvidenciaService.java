package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.dispute.dto.AdjuntarEvidenciaRequest;
import com.coffeecommits.brakket.dispute.dto.EvidenciaResponse;

import java.util.List;

public interface EvidenciaService {

    EvidenciaResponse adjuntar(Long disputaId, String correo, boolean esAdmin,
                               AdjuntarEvidenciaRequest request);

    List<EvidenciaResponse> listar(Long disputaId, String correo, boolean esAdmin);
}