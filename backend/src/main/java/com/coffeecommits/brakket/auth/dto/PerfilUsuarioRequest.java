package com.coffeecommits.brakket.auth.dto;

import com.coffeecommits.brakket.auth.model.VisibilidadPerfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solo el nombre visible es obligatorio; el resto del perfil se puede
 * completar de a poco. Si la visibilidad viene null se mantiene la actual.
 */
public record PerfilUsuarioRequest(
        @NotBlank(message = "El nombre visible es obligatorio")
        @Size(max = 120, message = "El nombre visible no puede superar 120 caracteres")
        String nombre,

        @Size(max = 500, message = "El avatar no puede superar 500 caracteres")
        String foto,

        @Size(max = 2000, message = "La biografía no puede superar 2000 caracteres")
        String biografia,

        @Size(max = 2000, message = "Las redes sociales no pueden superar 2000 caracteres")
        String redesSociales,

        VisibilidadPerfil visibilidadPerfil,

        List<Long> juegoIds
) {
}
