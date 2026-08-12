package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.config.TwitchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

/**
 * App Access Token de Twitch (client_credentials), compartido por todo lo que
 * se autentica contra la plataforma.
 *
 * <p>Nació dentro de {@link HelixClient} y se extrajo cuando el catálogo de
 * juegos pasó a IGDB: IGDB se autentica con este MISMO token y el mismo
 * Client-Id, así que tener dos cachés en memoria (una por consumidor) sería
 * pedir el doble de tokens para nada.</p>
 *
 * <p>El token se cachea hasta poco antes de su expiración; pedir uno nuevo en
 * cada llamada duplicaba latencia y consumía cuota del endpoint de auth.</p>
 */
@Component
@RequiredArgsConstructor
public class TwitchTokenProvider {

    /** Margen para renovar el token antes de que expire de verdad. */
    private static final Duration MARGEN_EXPIRACION = Duration.ofMinutes(5);

    private final TwitchProperties properties;
    private final RestClient.Builder restClientBuilder;

    private volatile String token;
    private volatile Instant tokenExpiraEn = Instant.EPOCH;

    /**
     * Token vigente, del caché mientras no haya expirado. Con
     * {@code forzarRenovacion} se pide uno nuevo aunque el cacheado parezca
     * válido: es la salida cuando Twitch responde 401 porque lo revocó antes
     * de tiempo.
     */
    public synchronized String obtener(boolean forzarRenovacion) {
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

    /** Hay credenciales para pedir token; sin esto no se sale a la red. */
    public boolean configurado() {
        return properties.isConfigured();
    }

    /** El Client-Id acompaña al bearer en toda llamada, a Helix y a IGDB. */
    public String clientId() {
        return properties.getClientId();
    }

    /**
     * Timeouts cortos (brakket.twitch.*): la petición del token corre dentro
     * del request del usuario, y sin límite un Twitch colgado retiene el hilo
     * de Tomcat y la conexión de Postgres indefinidamente.
     */
    private SimpleClientHttpRequestFactory fabricaConTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return factory;
    }
}
