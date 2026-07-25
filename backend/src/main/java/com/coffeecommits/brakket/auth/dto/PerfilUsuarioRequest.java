package com.coffeecommits.brakket.auth.dto;

import com.coffeecommits.brakket.auth.model.VisibilidadPerfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Solo el nombre visible es obligatorio; el resto del perfil se puede
 * completar de a poco. Si la visibilidad viene null se mantiene la actual.
 *
 * <p>Los campos de "ajustes personales" (RF-18) son datos privados del
 * usuario: solo los ve el dueño de la cuenta, no el perfil público.</p>
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

        List<Long> juegoIds,

        // ----- Ajustes personales -----

        @Size(max = 160, message = "El nombre completo no puede superar 160 caracteres")
        String nombreCompleto,

        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Size(max = 25, message = "El teléfono no puede superar 25 caracteres")
        String telefono,

        @Size(max = 80, message = "El país no puede superar 80 caracteres")
        String pais,

        @Size(max = 120, message = "La ciudad no puede superar 120 caracteres")
        String ciudad,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion,

        @Size(max = 20, message = "El código postal no puede superar 20 caracteres")
        String codigoPostal,

        @Size(max = 64, message = "La zona horaria no puede superar 64 caracteres")
        String zonaHoraria
) {
}
