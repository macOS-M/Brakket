package com.coffeecommits.brakket.dispute.dto;

import jakarta.validation.constraints.Size;

public record ResolverApelacionRequest(

        @Size(max = 1000, message = "La decision final no puede superar los 1000 caracteres")
        String decisionFinal,

        /** Si el comisionado quiere corregir el ganador una vez más. */
        Long equipoGanadorId
) {
}