package com.coffeecommits.brakket.dispute.dto;

import com.coffeecommits.brakket.dispute.model.Apelacion;

import java.time.LocalDateTime;

public record ApelacionResponse(
        Long id,
        Long disputaId,
        Long apeladaPorId,
        String apeladaPorNombre,
        String motivo,
        String estado,
        String decisionFinal,
        String comisionadoNombre,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaResolucion
) {

    public static ApelacionResponse fromEntity(Apelacion a) {
        return new ApelacionResponse(
                a.getId(),
                a.getDisputa().getId(),
                a.getApeladaPor() == null ? null : a.getApeladaPor().getId(),
                a.getApeladaPor() == null ? null : a.getApeladaPor().getNombre(),
                a.getMotivo(),
                a.getEstado(),
                a.getDecisionFinal(),
                a.getComisionado() == null ? null : a.getComisionado().getNombre(),
                a.getFechaCreacion(),
                a.getFechaResolucion()
        );
    }
}