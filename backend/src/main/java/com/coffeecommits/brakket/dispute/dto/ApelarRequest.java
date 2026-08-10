package com.coffeecommits.brakket.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApelarRequest(

        @NotBlank(message = "El motivo de la apelacion es obligatorio")
        @Size(max = 500)
        String motivo
) {
}