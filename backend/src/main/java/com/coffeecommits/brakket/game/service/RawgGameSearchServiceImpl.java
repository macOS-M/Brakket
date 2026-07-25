package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.game.dto.JuegoExternoDetalle;
import com.coffeecommits.brakket.game.dto.JuegoExternoResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
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
        // Sin timeouts explícitos, una red "agujero negro" (TCP que nunca
        // responde) colgaría el seeder del arranque y los threads de /top.
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                        .build(ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(Duration.ofSeconds(5))
                                .withReadTimeout(Duration.ofSeconds(10))))
                .build();
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

    /** Cache simple: los "top" de RAWG cambian poco y protegen la cuota. */
    private static final long CACHE_OK_MS = 60 * 60 * 1000;
    /** Un fallo/vacío también se recuerda: si RAWG está caído, el endpoint
     *  público no debe pegarle a la API en cada visita anónima. */
    private static final long CACHE_FALLO_MS = 5 * 60 * 1000;
    private volatile List<JuegoExternoResponse> popularesCache = List.of();
    private volatile long popularesCacheMs = 0;
    private volatile boolean popularesCachePoblado = false;

    @Override
    public List<JuegoExternoResponse> populares() {
        if (!disponible()) {
            throw new BusinessException(
                    "El buscador externo no está configurado (falta RAWG_API_KEY en el backend)");
        }
        long ahora = System.currentTimeMillis();
        long vigencia = popularesCache.isEmpty() ? CACHE_FALLO_MS : CACHE_OK_MS;
        if (popularesCachePoblado && ahora - popularesCacheMs < vigencia) {
            return popularesCache;
        }
        // Ordenado por popularidad (cantidad de usuarios que lo agregaron).
        List<JuegoExternoResponse> resultado;
        try {
            resultado = consultar(uri -> uri.path("/games")
                    .queryParam("key", apiKey)
                    .queryParam("ordering", "-added")
                    .queryParam("page_size", 24)
                    .build());
        } catch (BusinessException e) {
            popularesCache = List.of();
            popularesCacheMs = ahora;
            popularesCachePoblado = true;
            throw e;
        }
        popularesCache = resultado;
        popularesCacheMs = ahora;
        popularesCachePoblado = true;
        return resultado;
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
                        juego.slug(),
                        juego.nombre(),
                        traducirGenero(juego),
                        juego.imagen()))
                .toList();
    }

    /**
     * Ficha completa + capturas: dos llamadas que se hacen UNA vez por juego
     * (al importarlo); después todo se sirve desde nuestra BD. Nunca tumba
     * el import: si RAWG falla, el juego entra con los datos básicos.
     */
    @Override
    public JuegoExternoDetalle detalle(String slug) {
        if (!disponible() || slug == null || slug.isBlank()) {
            return null;
        }
        try {
            RawgDetalle d = restClient.get()
                    .uri(uri -> uri.path("/games/{slug}").queryParam("key", apiKey).build(slug))
                    .retrieve().body(RawgDetalle.class);
            if (d == null) {
                return null;
            }
            RawgCapturas capturas = restClient.get()
                    .uri(uri -> uri.path("/games/{slug}/screenshots")
                            .queryParam("key", apiKey).queryParam("page_size", 6).build(slug))
                    .retrieve().body(RawgCapturas.class);

            return new JuegoExternoDetalle(
                    recortar(d.descripcion(), 3900),
                    parsearFecha(d.released()),
                    d.rating(),
                    d.metacritic(),
                    recortar(d.website(), 300),
                    d.platforms() == null ? List.of() : d.platforms().stream()
                            .map(p -> p.platform() == null ? null : p.platform().name())
                            .filter(n -> n != null && !n.isBlank())
                            .limit(8)
                            .toList(),
                    d.tags() == null ? List.of() : d.tags().stream()
                            .filter(t -> t.language() == null || "eng".equals(t.language()))
                            .map(RawgTag::name)
                            .filter(n -> n != null && !n.isBlank())
                            .limit(8)
                            .toList(),
                    capturas == null || capturas.results() == null ? List.of()
                            : capturas.results().stream()
                                    .map(RawgCaptura::image)
                                    .filter(u -> u != null && !u.isBlank())
                                    .limit(6)
                                    .toList());
        } catch (RestClientException e) {
            return null;
        }
    }

    private static String recortar(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    private static java.time.LocalDate parsearFecha(String released) {
        try {
            return released == null ? null : java.time.LocalDate.parse(released);
        } catch (Exception e) {
            return null;
        }
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
    record RawgJuego(String slug,
                     @JsonProperty("name") String nombre,
                     @JsonProperty("background_image") String imagen,
                     List<RawgGenero> genres) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgGenero(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgDetalle(@JsonProperty("description_raw") String descripcion,
                       String released,
                       Double rating,
                       Integer metacritic,
                       String website,
                       List<RawgPlataformaWrap> platforms,
                       List<RawgTag> tags) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgPlataformaWrap(RawgPlataforma platform) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgPlataforma(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgTag(String name, String language) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgCapturas(List<RawgCaptura> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgCaptura(String image) {
    }
}
