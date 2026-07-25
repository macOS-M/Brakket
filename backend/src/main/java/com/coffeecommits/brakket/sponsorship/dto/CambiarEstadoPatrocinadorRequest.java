package com.coffeecommits.brakket.sponsorship.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoPatrocinadorRequest(

        @NotNull(message = "Debes indicar el nuevo estado")
        Boolean activo
) {
}