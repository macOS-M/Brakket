package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.config.TwitchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;

/**
 * Cliente HTTP compartido contra la API Helix de Twitch (EPIC-10, RNF-22/23).
 *
 * <p>Centraliza el App Access Token (client_credentials): antes cada llamada a
 * Helix pedía un token nuevo, duplicando latencia y consumiendo cuota del
 * endpoint de auth. Aquí el token se cachea en memoria hasta poco antes de su
 * expiración y, si Twitch responde 401 (token revocado), se renueva una única
 * vez y se reintenta la llamada.</p>
 */
@Component
@RequiredArgsConstructor
public class HelixClient {
    /** Margen para renovar el token antes de que expire de verdad. */
    private static final Duration MARGEN_EXPIRACION = Duration.ofMinutes(5);

    private final TwitchProperties properties;
    private final RestClient.Builder restClientBuilder;

    private volatile String token;
    private volatile Instant tokenExpiraEn = Instant.EPOCH;

    /**
     * GET a Helix con el path indicado (incluida la query string).
     * Lanza {@link TwitchUnavailableException} ante cualquier fallo de red o
     * de la API que no sea recuperable renovando el token.
     */
    public JsonNode get(String path) {
        if (!properties.isConfigured()) {
            throw new BusinessException("Configure TWITCH_CLIENT_ID y TWITCH_CLIENT_SECRET antes de consultar Twitch.");
        }
        try {
            try {
                return ejecutar(path, obtenerToken(false));
            } catch (HttpClientErrorException.Unauthorized ex) {
                // Token expirado o revocado por Twitch: renovar UNA vez y reintentar.
                return ejecutar(path, obtenerToken(true));
            }
        } catch (RestClientException ex) {
            throw new TwitchUnavailableException("Twitch rechazó la conexión o no está disponible.", ex);
        }
    }

    private JsonNode ejecutar(String path, String bearer) {
        return restClientBuilder.baseUrl(properties.getApiBaseUrl())
                .requestFactory(fabricaConTimeouts()).build().get().uri(path)
                .header("Client-Id", properties.getClientId())
                .header("Authorization", "Bearer " + bearer)
                .retrieve().body(JsonNode.class);
    }

    private synchronized String obtenerToken(boolean forzarRenovacion) {
        if (!forzarRenovacion && token != null && Instant.now().isBefore(tokenExpiraEn)) {
            return token;
        }
        // Credenciales en el BODY del form, nunca en la query string: la URL
        // viaja en excepciones, logs y proxies, y ahí el secret se filtraría
        // con cualquier stacktrace de un token request fallido.
        MultiValueMap<String, String> credenciales = new LinkedMultiValueMap<>();
        credenciales.add("client_id", properties.getClientId());
        credenciales.add("client_secret", properties.getClientSecret());
        credenciales.add("grant_type", "client_credentials");

        RestClient auth = restClientBuilder.baseUrl(properties.getAuthBaseUrl())
                .requestFactory(fabricaConTimeouts()).build();
        JsonNode respuesta = auth.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(credenciales)
                .retrieve().body(JsonNode.class);
        token = respuesta == null ? "" : respuesta.path("access_token").asText();
        long expiraSegundos = respuesta == null ? 0 : respuesta.path("expires_in").asLong();
        tokenExpiraEn = Instant.now().plusSeconds(expiraSegundos).minus(MARGEN_EXPIRACION);
        return token;
    }

    /**
     * Timeouts cortos (configurables vía brakket.twitch.*): la llamada corre
     * dentro del request (y de su transacción); sin límite, un Twitch colgado
     * retiene el hilo de Tomcat y la conexión de Postgres indefinidamente.
     */
    private SimpleClientHttpRequestFactory fabricaConTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return factory;
    }
}
