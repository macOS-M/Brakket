package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF-10: la causa de la expulsion es obligatoria. */
public record ExpulsarIntegranteRequest(
        @NotBlank(message = "La causa de la expulsion es obligatoria")
        @Size(max = 500, message = "La causa no puede superar los 500 caracteres")
        String causa
) {
}
