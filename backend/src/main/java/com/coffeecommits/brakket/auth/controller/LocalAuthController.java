package com.coffeecommits.brakket.auth.controller;

import com.coffeecommits.brakket.auth.dto.LoginLocalRequest;
import com.coffeecommits.brakket.auth.dto.RegistroLocalRequest;
import com.coffeecommits.brakket.auth.dto.TokenResponse;
import com.coffeecommits.brakket.auth.service.LocalAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro e inicio de sesión con correo y contraseña (DD-04). Ambos son
 * públicos (ver SecurityConfig) y devuelven el mismo JWT que el flujo de
 * Google: el frontend lo persiste y sigue idéntico.
 */
@RestController
@RequestMapping("/api/auth")
public class LocalAuthController {

    private final LocalAuthService localAuthService;

    public LocalAuthController(LocalAuthService localAuthService) {
        this.localAuthService = localAuthService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registrar(@Valid @RequestBody RegistroLocalRequest request) {
        return new TokenResponse(localAuthService.registrar(request));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginLocalRequest request) {
        return new TokenResponse(localAuthService.login(request));
    }
}
