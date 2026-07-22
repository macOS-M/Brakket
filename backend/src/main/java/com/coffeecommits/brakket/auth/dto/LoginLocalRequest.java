package com.coffeecommits.brakket.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Inicio de sesión con correo y contraseña (DD-04). */
public record LoginLocalRequest(
        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
