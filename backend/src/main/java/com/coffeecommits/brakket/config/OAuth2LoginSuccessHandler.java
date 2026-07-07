package com.coffeecommits.brakket.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Al completarse el login con Google, genera un JWT propio de Brakket y redirige
 * al frontend con el token para que la SPA lo guarde.
 *
 * <p>TODO (EPIC-01): antes de generar el token, crear/actualizar el usuario en la
 * BD (tabla usuario) y asignarle su rol base.</p>
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${brakket.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String correo = user.getAttribute("email");
        String nombre = user.getAttribute("name");

        String token = jwtService.generateToken(correo, Map.of("name", nombre == null ? "" : nombre));

        String target = frontendUrl + "/auth/callback?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
