package com.coffeecommits.brakket.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdjuntarEvidenciaRequest(

        @NotBlank(message = "La URL de la evidencia es obligatoria")
        @Size(max = 500)
        String url,

        @Size(max = 500)
        String descripcion
) {
}