package com.coffeecommits.brakket.transmision.service;

import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.twitch.service.HelixClient;
import com.coffeecommits.brakket.twitch.service.TwitchTokenProvider;
import com.coffeecommits.brakket.twitch.service.TwitchUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del adaptador de Twitch con HTTP real contra un servidor local
 * (com.sun.net.httpserver, sin dependencias nuevas): cubre live, offline,
 * 401 con renovación de token, 500 y timeout.
 */
class TwitchStreamProviderTest {

    private static final String TOKEN_OK = "{\"access_token\":\"tok-1\",\"expires_in\":3600,\"token_type\":\"bearer\"}";
    private static final String STREAM_VIVO = """
            {"data":[{"id":"999","user_id":"123","user_login":"brakketcenfotec",
            "title":"Gran final","viewer_count":42,"game_name":"League of Legends","language":"es",
            "started_at":"2026-07-23T18:00:00Z",
            "thumbnail_url":"https://static-cdn.jtvnw.net/previews-ttv/live_user_brakketcenfotec-{width}x{height}.jpg"}]}""";

    private HttpServer server;
    private TwitchProperties properties;
    private TwitchStreamProvider provider;
    private final AtomicInteger tokensPedidos = new AtomicInteger();

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/oauth2/token", exchange -> {
            tokensPedidos.incrementAndGet();
            responder(exchange, 200, TOKEN_OK);
        });
        server.start();

        properties = new TwitchProperties();
        properties.setClientId("client");
        properties.setClientSecret("secret");
        String base = "http://localhost:" + server.getAddress().getPort();
        properties.setAuthBaseUrl(base + "/oauth2");
        properties.setApiBaseUrl(base + "/helix");
        // Timeouts cortos para que el caso "Twitch colgado" no frene la suite.
        properties.setConnectTimeoutMs(500);
        properties.setReadTimeoutMs(500);
        // El token vive en TwitchTokenProvider (lo comparte con IGDB); acá se
        // le pasa uno real para seguir cubriendo cacheo y renovación por 401.
        TwitchTokenProvider tokenProvider = new TwitchTokenProvider(properties, RestClient.builder());
        provider = new TwitchStreamProvider(new HelixClient(properties, tokenProvider, RestClient.builder()));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapeaDirectoEnVivoYReemplazaElThumbnail() {
        server.createContext("/helix/streams", exchange -> responder(exchange, 200, STREAM_VIVO));

        List<StreamProvider.StreamEnVivo> directos = provider.getLiveStreams(List.of("brakketcenfotec"));

        assertThat(directos).hasSize(1);
        StreamProvider.StreamEnVivo directo = directos.get(0);
        assertThat(directo.titulo()).isEqualTo("Gran final");
        assertThat(directo.espectadores()).isEqualTo(42);
        assertThat(directo.categoria()).isEqualTo("League of Legends");
        assertThat(directo.idioma()).isEqualTo("es");
        // Placeholder {width}x{height} sustituido y cache-buster añadido:
        // el thumbnail de Twitch se refresca cada ~5 min sin cambiar de URL.
        assertThat(directo.thumbnailUrl()).contains("-1280x720.jpg").contains("?cb=");
    }

    @Test
    void canalSinDirectoDevuelveListaVacia() {
        server.createContext("/helix/streams", exchange -> responder(exchange, 200, "{\"data\":[]}"));

        assertThat(provider.getLiveStreams(List.of("brakketcenfotec"))).isEmpty();
    }

    @Test
    void ante401RenuevaElTokenYReintentaUnaVez() {
        AtomicInteger llamadas = new AtomicInteger();
        server.createContext("/helix/streams", exchange -> {
            if (llamadas.incrementAndGet() == 1) {
                responder(exchange, 401, "{\"error\":\"Unauthorized\"}");
            } else {
                responder(exchange, 200, STREAM_VIVO);
            }
        });

        List<StreamProvider.StreamEnVivo> directos = provider.getLiveStreams(List.of("brakketcenfotec"));

        assertThat(directos).hasSize(1);
        assertThat(llamadas.get()).isEqualTo(2);
        assertThat(tokensPedidos.get()).isEqualTo(2);
    }

    @Test
    void error500SeTraduceATwitchNoDisponible() {
        server.createContext("/helix/streams", exchange -> responder(exchange, 500, "{\"error\":\"boom\"}"));

        assertThatThrownBy(() -> provider.getLiveStreams(List.of("brakketcenfotec")))
                .isInstanceOf(TwitchUnavailableException.class);
    }

    @Test
    void timeoutSeTraduceATwitchNoDisponible() {
        server.createContext("/helix/streams", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            responder(exchange, 200, "{\"data\":[]}");
        });

        assertThatThrownBy(() -> provider.getLiveStreams(List.of("brakketcenfotec")))
                .isInstanceOf(TwitchUnavailableException.class);
    }

    @Test
    void reutilizaElTokenCacheadoEntreLlamadas() {
        server.createContext("/helix/streams", exchange -> responder(exchange, 200, "{\"data\":[]}"));

        provider.getLiveStreams(List.of("brakketcenfotec"));
        provider.getLiveStreams(List.of("brakketcenfotec"));

        assertThat(tokensPedidos.get()).isEqualTo(1);
    }

    @Test
    void mapeaCanalesYUltimoVod() {
        server.createContext("/helix/users", exchange -> responder(exchange, 200, """
                {"data":[{"id":"123","login":"brakketcenfotec","display_name":"BrakketCenfotec",
                "profile_image_url":"https://cdn/avatar.png","offline_image_url":"https://cdn/offline.png"}]}"""));
        server.createContext("/helix/videos", exchange -> responder(exchange, 200, """
                {"data":[{"id":"v1","url":"https://www.twitch.tv/videos/v1","title":"VOD final",
                "thumbnail_url":"https://cdn/vod-%{width}x%{height}.jpg","duration":"2h10m",
                "published_at":"2026-07-20T20:00:00Z"}]}"""));

        List<StreamProvider.CanalStream> canales = provider.getChannels(List.of("brakketcenfotec"));
        StreamProvider.VodInfo vod = provider.getLatestVod("123");

        assertThat(canales).hasSize(1);
        assertThat(canales.get(0).nombreMostrado()).isEqualTo("BrakketCenfotec");
        assertThat(canales.get(0).avatarUrl()).isEqualTo("https://cdn/avatar.png");
        assertThat(vod.titulo()).isEqualTo("VOD final");
        // El placeholder de los VOD es %{width}x%{height}, distinto al de /streams.
        assertThat(vod.thumbnailUrl()).contains("vod-1280x720.jpg");
    }

    private void responder(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
