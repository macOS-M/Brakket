package com.coffeecommits.brakket.auth.dto;

import java.util.List;

/**
 * Datos del usuario autenticado que consume el frontend en {@code GET /api/me}.
 * La forma coincide con la interfaz {@code Usuario} de la SPA (Angular).
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
        List<String> roles
) {
}
