package com.coffeecommits.brakket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS para permitir que el frontend Angular consuma la API.
 *
 * <p>El origen permitido se configura con {@code brakket.frontend-url}
 * (variable FRONTEND_URL) y admite <b>una lista separada por comas</b> y
 * <b>comodines</b>, porque para la demo el frontend se sirve por un túnel de
 * dominio cambiante (ngrok o el Port Forwarding de VS Code). Ejemplos:</p>
 * <pre>
 *   FRONTEND_URL=http://localhost:4200
 *   FRONTEND_URL=http://localhost:4200,https://*.ngrok-free.dev
 *   FRONTEND_URL=http://localhost:4200,https://*.devtunnels.ms
 * </pre>
 *
 * <p>Se usa {@code setAllowedOriginPatterns} (no {@code setAllowedOrigins})
 * porque es el único que combina comodines con {@code allowCredentials(true)}:
 * el navegador exige que el servidor devuelva el origen concreto, y este método
 * lo refleja a partir del patrón.</p>
 */
@Configuration
public class CorsConfig {

    @Value("${brakket.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> patrones = Arrays.stream(frontendUrl.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(patrones);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
