package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.game.dto.JuegoExternoResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Implementación contra la API pública de RAWG (rawg.io).
 *
 * <p>La key vive en el backend ({@code RAWG_API_KEY} del .env) y nunca viaja
 * al navegador; el frontend consume nuestro proxy autenticado. Los géneros de
 * RAWG vienen en inglés: se traducen los comunes y el resto se deja tal cual
 * (el formulario permite editarlo antes de guardar).</p>
 */
@Service
public class RawgGameSearchServiceImpl implements ExternalGameSearchService {

    private static final int MAX_RESULTADOS = 12;

    private static final Map<String, String> GENEROS = Map.ofEntries(
            Map.entry("Action", "Acción"),
            Map.entry("Adventure", "Aventura"),
            Map.entry("Arcade", "Arcade"),
            Map.entry("Board Games", "Juegos de mesa"),
            Map.entry("Card", "Cartas"),
            Map.entry("Casual", "Casual"),
            Map.entry("Family", "Familiar"),
            Map.entry("Fighting", "Lucha"),
            Map.entry("Indie", "Indie"),
            Map.entry("Massively Multiplayer", "Multijugador masivo"),
            Map.entry("Platformer", "Plataformas"),
            Map.entry("Puzzle", "Puzzle"),
            Map.entry("RPG", "RPG"),
            Map.entry("Racing", "Carreras"),
            Map.entry("Shooter", "Shooter"),
            Map.entry("Simulation", "Simulación"),
            Map.entry("Sports", "Deportes"),
            Map.entry("Strategy", "Estrategia")
    );

    private final RestClient restClient;
    private final String apiKey;

    public RawgGameSearchServiceImpl(RestClient.Builder restClientBuilder,
                                     @Value("${brakket.rawg.base-url}") String baseUrl,
                                     @Value("${brakket.rawg.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public boolean disponible() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public List<JuegoExternoResponse> buscar(String consulta) {
        if (!disponible()) {
            throw new BusinessException(
                    "El buscador externo no está configurado (falta RAWG_API_KEY en el backend)");
        }
        String texto = consulta == null ? "" : consulta.trim();
        if (texto.length() < 2) {
            return List.of();
        }
        return consultar(uri -> uri.path("/games")
                .queryParam("key", apiKey)
                .queryParam("search", texto)
                .queryParam("page_size", MAX_RESULTADOS)
                .build());
    }

    @Override
    public List<JuegoExternoResponse> populares() {
        if (!disponible()) {
            throw new BusinessException(
                    "El buscador externo no está configurado (falta RAWG_API_KEY en el backend)");
        }
        // Ordenado por popularidad (cantidad de usuarios que lo agregaron).
        return consultar(uri -> uri.path("/games")
                .queryParam("key", apiKey)
                .queryParam("ordering", "-added")
                .queryParam("page_size", 24)
                .build());
    }

    private List<JuegoExternoResponse> consultar(
            java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
        RawgPage pagina;
        try {
            pagina = restClient.get().uri(uri).retrieve().body(RawgPage.class);
        } catch (RestClientException e) {
            throw new BusinessException("El buscador externo de juegos no está disponible ahora");
        }

        if (pagina == null || pagina.results() == null) {
            return List.of();
        }
        return pagina.results().stream()
                .filter(juego -> juego.nombre() != null && !juego.nombre().isBlank())
                .map(juego -> new JuegoExternoResponse(
                        juego.nombre(),
                        traducirGenero(juego),
                        juego.imagen()))
                .toList();
    }

    /** Primer género traducido; si RAWG no trae géneros, cadena vacía. */
    private static String traducirGenero(RawgJuego juego) {
        if (juego.genres() == null || juego.genres().isEmpty()) {
            return "";
        }
        String nombre = juego.genres().get(0).name();
        return GENEROS.getOrDefault(nombre, nombre == null ? "" : nombre);
    }

    // ---------- Forma mínima de la respuesta de RAWG ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgPage(List<RawgJuego> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgJuego(@JsonProperty("name") String nombre,
                     @JsonProperty("background_image") String imagen,
                     List<RawgGenero> genres) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgGenero(String name) {
    }
}
