package com.coffeecommits.brakket.sponsorship.dto;

import jakarta.validation.constraints.NotBlank;

public record EditarEspacioPublicitarioRequest(
        @NotBlank(message = "La ubicación es obligatoria")
        String ubicacion,

        @NotBlank(message = "La imagen es obligatoria")
        String imagenUrl,

        String enlaceUrl
) {
}