package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.NotBlank;

public record AsignarRolRequest(

        @NotBlank(message = "El nuevo rol es obligatorio")
        String nuevoRol
) {
}