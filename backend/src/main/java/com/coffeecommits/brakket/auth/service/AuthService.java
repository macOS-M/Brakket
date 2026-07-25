package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.dto.PerfilUsuarioRequest;
import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.model.Usuario;

/**
 * Lógica de autenticación y perfil de usuario (EPIC-01, RF-17/RF-18).
 */
public interface AuthService {

    /**
     * Crea o actualiza el usuario a partir de los datos que devuelve Google tras
     * el login, asignándole el rol base si es nuevo. Idempotente por {@code googleId}.
     */
    Usuario upsertGoogleUser(String googleId, String correo, String nombre, String fotoUrl);

    /** Devuelve el usuario autenticado (identificado por su correo) con sus roles. */
    UsuarioResponse getCurrentUser(String correo);

    /** Actualiza el perfil del usuario autenticado. */
    UsuarioResponse updateCurrentUser(String correo, PerfilUsuarioRequest request);
}
