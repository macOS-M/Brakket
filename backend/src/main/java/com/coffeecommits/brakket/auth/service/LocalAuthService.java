package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.dto.LoginLocalRequest;
import com.coffeecommits.brakket.auth.dto.RegistroLocalRequest;

/**
 * Registro e inicio de sesión con correo y contraseña (DD-04, complemento
 * de RF-17). Emite el mismo JWT que el flujo de Google.
 */
public interface LocalAuthService {

    /** Registra una cuenta local y devuelve su JWT. */
    String registrar(RegistroLocalRequest request);

    /** Valida credenciales y devuelve el JWT. */
    String login(LoginLocalRequest request);
}
