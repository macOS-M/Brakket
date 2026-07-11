package com.coffeecommits.brakket.auth.controller;

import com.coffeecommits.brakket.auth.dto.PerfilUsuarioRequest;
import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticación/perfil (EPIC-01).
 *
 * <p>El login con Google lo maneja Spring Security (redirección OAuth2); aquí solo
 * se expone el usuario autenticado a partir del JWT que la SPA envía en cada request.</p>
 */
@RestController
@RequestMapping("/api")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Usuario autenticado. El {@code Authentication} lo puebla el JwtAuthenticationFilter. */
    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }

    @PutMapping("/me")
    public UsuarioResponse updateMe(Authentication authentication, @Valid @RequestBody PerfilUsuarioRequest request) {
        return authService.updateCurrentUser(authentication.getName(), request);
    }
}
