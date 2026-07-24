package com.coffeecommits.brakket.transmision.service;

import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.service.HelixClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de {@link StreamProvider} sobre la API Helix de Twitch.
 *
 * <p>Batching: /users y /streams aceptan hasta 100 logins por request; se
 * consulta todo en una sola llamada para respetar la cuota del token
 * (RNF-22). El tamaño de thumbnail se fija aquí y se añade un cache-buster
 * porque Twitch regenera la imagen cada ~5 minutos pero la URL no cambia.</p>
 */
@Component
@RequiredArgsConstructor
public class TwitchStreamProvider implements StreamProvider {
    /** Límite documentado de logins por request en /users y /streams. */
    private static final int MAX_BATCH = 100;
    /** Tamaño real que sustituye el placeholder {width}x{height} del thumbnail. */
    private static final String TAMANO_THUMBNAIL = "1280x720";
    /** El thumbnail se refresca cada ~5 min; el buster cambia con esa cadencia. */
    private static final long VENTANA_CACHE_BUSTER_SEGUNDOS = 300;

    private final HelixClient helixClient;

    @Override
    public PlataformaTransmision plataforma() {
        return PlataformaTransmision.TWITCH;
    }

    @Override
    public List<CanalStream> getChannels(List<String> handles) {
        if (handles.isEmpty()) return List.of();
        JsonNode data = helixClient.get("/users?" + query("login", handles)).path("data");
        List<CanalStream> canales = new ArrayList<>();
        data.forEach(user -> canales.add(new CanalStream(
                user.path("id").asText(),
                user.path("login").asText(),
                user.path("display_name").asText(),
                user.path("profile_image_url").asText(null),
                user.path("offline_image_url").asText(null))));
        return canales;
    }

    @Override
    public List<StreamEnVivo> getLiveStreams(List<String> handles) {
        if (handles.isEmpty()) return List.of();
        JsonNode data = helixClient.get("/streams?" + query("user_login", handles)).path("data");
        List<StreamEnVivo> directos = new ArrayList<>();
        data.forEach(stream -> directos.add(new StreamEnVivo(
                stream.path("id").asText(),
                stream.path("user_id").asText(),
                stream.path("user_login").asText(),
                stream.path("title").asText(),
                stream.path("viewer_count").asInt(),
                thumbnail(stream.path("thumbnail_url").asText(null), "{width}x{height}"),
                stream.path("game_name").asText(null),
                stream.path("language").asText(null),
                OffsetDateTime.parse(stream.path("started_at").asText()).toLocalDateTime())));
        return directos;
    }

    @Override
    public VodInfo getLatestVod(String userId) {
        JsonNode data = helixClient
                .get("/videos?user_id=" + userId + "&type=archive&first=1").path("data");
        if (!data.isArray() || data.isEmpty()) return null;
        JsonNode vod = data.get(0);
        return new VodInfo(
                vod.path("id").asText(),
                vod.path("url").asText(),
                vod.path("title").asText(),
                // Los VOD usan %{width}x%{height}, distinto del placeholder de /streams.
                thumbnail(vod.path("thumbnail_url").asText(null), "%{width}x%{height}"),
                vod.path("duration").asText(null),
                OffsetDateTime.parse(vod.path("published_at").asText()).toLocalDateTime());
    }

    private String query(String parametro, List<String> valores) {
        return valores.stream().limit(MAX_BATCH)
                .map(v -> parametro + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String thumbnail(String plantilla, String placeholder) {
        if (plantilla == null || plantilla.isBlank()) return null;
        long buster = Instant.now().getEpochSecond() / VENTANA_CACHE_BUSTER_SEGUNDOS;
        return plantilla.replace(placeholder, TAMANO_THUMBNAIL) + "?cb=" + buster;
    }
}
