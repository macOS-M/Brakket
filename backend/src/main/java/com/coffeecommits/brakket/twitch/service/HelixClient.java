package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.config.TwitchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Cliente HTTP compartido contra la API Helix de Twitch (EPIC-10, RNF-22/23).
 *
 * <p>El App Access Token vive en {@link TwitchTokenProvider}, que lo cachea y
 * lo comparte con los demás consumidores de la plataforma. Acá queda la parte
 * propia de Helix: si Twitch responde 401 (token revocado), se renueva una
 * única vez y se reintenta la llamada.</p>
 */
@Component
@RequiredArgsConstructor
public class HelixClient {

    private final TwitchProperties properties;
    private final TwitchTokenProvider tokenProvider;
    private final RestClient.Builder restClientBuilder;

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
                return ejecutar(path, tokenProvider.obtener(false));
            } catch (HttpClientErrorException.Unauthorized ex) {
                // Token expirado o revocado por Twitch: renovar UNA vez y reintentar.
                return ejecutar(path, tokenProvider.obtener(true));
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
