package com.coffeecommits.brakket.config;

import com.coffeecommits.brakket.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Al completarse el login con Google, crea/actualiza el usuario en la BD (con su
 * rol base), genera un JWT propio de Brakket y redirige al frontend con el token
 * para que la SPA lo guarde.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AuthService authService;

    @Value("${brakket.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(JwtService jwtService, AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String googleId = user.getAttribute("sub");
        String correo = user.getAttribute("email");
        String nombre = user.getAttribute("name");
        String fotoUrl = user.getAttribute("picture");

        // Alta/actualización del usuario y asignación de rol base (RF-17).
        var usuario = authService.upsertGoogleUser(googleId, correo, nombre, fotoUrl);

        // Cuenta bloqueada (RF-19): no se emite JWT; se devuelve al login con el motivo.
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            String mensaje = URLEncoder.encode(
                    "La cuenta está bloqueada. Contacte a un administrador.", StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/login?error=cuenta_bloqueada&error_message=" + mensaje);
            return;
        }

        String token = jwtService.generateToken(correo, Map.of("name", nombre == null ? "" : nombre));

        String target = frontendUrl + "/auth/callback?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
