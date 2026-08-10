package com.coffeecommits.brakket.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverDisputaRequest(

        @NotBlank(message = "La decision es obligatoria")
        String decision,

        @NotBlank(message = "La justificacion es obligatoria")
        @Size(max = 1000)
        String justificacion,

        @Size(max = 500)
        String sancion,

        Long equipoGanadorId
) {
}