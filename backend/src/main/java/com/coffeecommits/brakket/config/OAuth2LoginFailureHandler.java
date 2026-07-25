package com.coffeecommits.brakket.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redirige los errores de OAuth2 al login de la SPA para evitar que el backend
 * muestre una Whitelabel 404.
 */
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${brakket.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 login failed: {} - {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);

        String error = request.getParameter("error");
        String description = request.getParameter("error_description");
        String exceptionName = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();

        StringBuilder target = new StringBuilder(frontendUrl)
                .append("/auth/login?error=")
                .append(URLEncoder.encode(exceptionName, StandardCharsets.UTF_8));
        if (exceptionMessage != null && !exceptionMessage.isBlank()) {
            target.append("&error_message=")
                    .append(URLEncoder.encode(exceptionMessage, StandardCharsets.UTF_8));
        }
        if (error != null && !error.isBlank()) {
            target.append("&oauth_error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        }
        if (description != null && !description.isBlank()) {
            target.append("&oauth_error_description=")
                    .append(URLEncoder.encode(description, StandardCharsets.UTF_8));
        }

        getRedirectStrategy().sendRedirect(request, response, target.toString());
    }
}