package com.coffeecommits.brakket.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record AdjuntarEvidenciaRequest(

        @NotBlank(message = "La URL de la evidencia es obligatoria")
        @URL(message = "La evidencia debe ser una URL valida")
        @Size(max = 500)
        String url,

        @Size(max = 500)
        String descripcion
) {
}