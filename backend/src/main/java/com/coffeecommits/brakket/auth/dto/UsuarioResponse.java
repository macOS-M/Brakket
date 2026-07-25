package com.coffeecommits.brakket.auth.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos del usuario autenticado que consume el frontend en {@code GET /api/me}.
 * La forma coincide con la interfaz {@code Usuario} de la SPA (Angular).
 *
 * <p>Incluye los ajustes personales (RF-18). Este DTO solo se devuelve al
 * dueño de la cuenta; el perfil público de otros usuarios se sirve por otros
 * endpoints y no expone estos campos.</p>
 */
public record UsuarioResponse(
        boolean authenticated,
        Long id,
        String nombre,
        String correo,
        String foto,
        String biografia,
        String redesSociales,
        String visibilidadPerfil,
        List<Long> juegoIds,
        List<String> roles,
        String nombreCompleto,
        LocalDate fechaNacimiento,
        String telefono,
        String pais,
        String ciudad,
        String direccion,
        String codigoPostal,
        String zonaHoraria
) {
}
