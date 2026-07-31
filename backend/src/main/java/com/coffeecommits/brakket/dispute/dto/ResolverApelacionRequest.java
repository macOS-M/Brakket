package com.coffeecommits.brakket.dispute.dto;

public record ResolverApelacionRequest(

        String decisionFinal,

        /** Si el comisionado quiere corregir el ganador una vez más. */
        Long equipoGanadorId
) {
}