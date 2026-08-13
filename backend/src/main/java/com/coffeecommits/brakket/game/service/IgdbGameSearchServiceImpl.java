package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.game.dto.JuegoExternoDetalle;
import com.coffeecommits.brakket.game.dto.JuegoExternoResponse;
import com.coffeecommits.brakket.twitch.service.TwitchTokenProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Implementación contra IGDB (api.igdb.com/v4), alternativa a RAWG.
 *
 * <p>Se eligió con {@code brakket.catalogo.proveedor=igdb} porque IGDB se
 * autentica con el MISMO App Access Token de Twitch que el proyecto ya emite
 * para las transmisiones (EPIC-10): no hay credenciales nuevas que gestionar,
 * solo se reutiliza {@link TwitchTokenProvider}.</p>
 *
 * <p>Dos diferencias de fondo con RAWG, que explican casi todo lo raro de esta
 * clase: las consultas van en el BODY en lenguaje APICalypse (no como query
 * params), y las imágenes llegan como {@code image_id} en vez de URL, así que
 * la URL se arma acá.</p>
 */
@Service
@ConditionalOnProperty(name = "brakket.catalogo.proveedor", havingValue = "igdb", matchIfMissing = true)
public class IgdbGameSearchServiceImpl implements ExternalGameSearchService {

    private static final int MAX_RESULTADOS = 12;

    /** Plantilla de las URLs de imagen; IGDB solo devuelve el id. */
    private static final String IMAGEN_BASE = "https://images.igdb.com/igdb/image/upload/";

    /**
     * IGDB permite 4 peticiones por segundo. El seeder recorre 20 títulos en
     * un for secuencial, y sin espaciarlas se pasa del límite y empieza a
     * recibir 429 a mitad del catálogo.
     */
    private static final long MS_ENTRE_LLAMADAS = 250;

    /**
     * Solo juegos "de verdad": game_type 0 descarta DLCs, expansiones y packs,
     * y version_parent nulo descarta las ediciones (Game of the Year, etc.)
     * que ensuciaban los resultados con el mismo título repetido.
     *
     * <p>Es game_type y NO category: IGDB retiró ese campo, y lo peligroso es
     * que filtrar por él no da error sino que devuelve 200 con lista vacía.</p>
     */
    private static final String SOLO_JUEGOS = "game_type = 0 & version_parent = null";

    /** Los géneros de IGDB vienen en inglés y con nombres propios suyos. */
    private static final Map<String, String> GENEROS = Map.ofEntries(
            Map.entry("Adventure", "Aventura"),
            Map.entry("Arcade", "Arcade"),
            Map.entry("Card & Board Game", "Juegos de mesa"),
            Map.entry("Fighting", "Lucha"),
            Map.entry("Hack and slash/Beat 'em up", "Acción"),
            Map.entry("Indie", "Indie"),
            Map.entry("MOBA", "MOBA"),
            Map.entry("Music", "Música"),
            Map.entry("Platform", "Plataformas"),
            Map.entry("Point-and-click", "Aventura gráfica"),
            Map.entry("Puzzle", "Puzzle"),
            Map.entry("Quiz/Trivia", "Trivia"),
            Map.entry("Racing", "Carreras"),
            Map.entry("Real Time Strategy (RTS)", "Estrategia en tiempo real"),
            Map.entry("Role-playing (RPG)", "RPG"),
            Map.entry("Shooter", "Shooter"),
            Map.entry("Simulator", "Simulación"),
            Map.entry("Sport", "Deportes"),
            Map.entry("Strategy", "Estrategia"),
            Map.entry("Tactical", "Táctico"),
            Map.entry("Turn-based strategy (TBS)", "Estrategia por turnos"),
            Map.entry("Visual Novel", "Novela visual")
    );

    /**
     * Temas de IGDB, que la ficha del juego muestra como etiquetas.
     *
     * <p>Es el catálogo completo de temas de IGDB, que es cerrado y corto (a
     * diferencia de los tags de RAWG, que son miles y abiertos). Se conservan
     * en inglés los préstamos ya asentados en español, con el mismo criterio
     * que {@link #GENEROS}, donde quedaron Arcade, Indie, MOBA o Shooter.</p>
     */
    private static final Map<String, String> TEMAS = Map.ofEntries(
            Map.entry("4X (explore, expand, exploit, and exterminate)", "4X"),
            Map.entry("Action", "Acción"),
            Map.entry("Business", "Negocios"),
            Map.entry("Comedy", "Comedia"),
            Map.entry("Drama", "Drama"),
            Map.entry("Educational", "Educativo"),
            Map.entry("Erotic", "Erótico"),
            Map.entry("Fantasy", "Fantasía"),
            Map.entry("Historical", "Histórico"),
            Map.entry("Horror", "Terror"),
            Map.entry("Kids", "Infantil"),
            Map.entry("Mystery", "Misterio"),
            Map.entry("Non-fiction", "No ficción"),
            Map.entry("Open world", "Mundo abierto"),
            Map.entry("Party", "Fiesta"),
            Map.entry("Romance", "Romance"),
            Map.entry("Sandbox", "Sandbox"),
            Map.entry("Science fiction", "Ciencia ficción"),
            Map.entry("Stealth", "Sigilo"),
            Map.entry("Survival", "Supervivencia"),
            Map.entry("Thriller", "Suspenso"),
            Map.entry("Warfare", "Bélico")
    );

    private final RestClient restClient;
    private final TwitchTokenProvider tokenProvider;

    private final Object cerrojoRitmo = new Object();
    private long ultimaLlamadaMs = 0;

    // Hay dos constructores (el de abajo es para el test), así que Spring
    // necesita que se le diga explícitamente cuál inyectar.
    @Autowired
    public IgdbGameSearchServiceImpl(RestClient.Builder restClientBuilder,
                                     @Value("${brakket.igdb.base-url}") String baseUrl,
                                     TwitchTokenProvider tokenProvider) {
        // Sin timeouts explícitos, una red "agujero negro" (TCP que nunca
        // responde) colgaría el seeder del arranque y los threads de /top.
        this(restClientBuilder
                        .baseUrl(baseUrl)
                        .requestFactory(ClientHttpRequestFactoryBuilder.detect()
                                .build(ClientHttpRequestFactorySettings.defaults()
                                        .withConnectTimeout(Duration.ofSeconds(5))
                                        .withReadTimeout(Duration.ofSeconds(10))))
                        .build(),
                tokenProvider);
    }

    /**
     * Para el test: recibe el cliente ya armado. El constructor de arriba fija
     * su propia request factory (los timeouts), y eso pisaría la que instala
     * MockRestServiceServer al enlazarse con el builder.
     */
    IgdbGameSearchServiceImpl(RestClient restClient, TwitchTokenProvider tokenProvider) {
        this.restClient = restClient;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean disponible() {
        return tokenProvider.configurado();
    }

    @Override
    public List<JuegoExternoResponse> buscar(String consulta) {
        if (!disponible()) {
            throw new BusinessException(
                    "El buscador externo no está configurado (faltan TWITCH_CLIENT_ID y TWITCH_CLIENT_SECRET en el backend)");
        }
        String texto = sanear(consulta);
        if (texto.length() < 2) {
            return List.of();
        }
        return consultar("search \"" + texto + "\";"
                + " fields name,slug,cover.image_id,genres.name;"
                + " where " + SOLO_JUEGOS + ";"
                + " limit " + MAX_RESULTADOS + ";")
                .stream()
                .filter(juego -> juego.nombre() != null && !juego.nombre().isBlank())
                .map(IgdbGameSearchServiceImpl::aResumen)
                // El ranking de IGDB no pone primero la coincidencia exacta:
                // buscando "Valorant" devuelve antes "Grit & Valor: 1949". El
                // seeder se queda con el primero, así que sin esto sembraría
                // el juego equivocado bajo el nombre correcto.
                .sorted(Comparator.comparing(juego -> !texto.equalsIgnoreCase(juego.nombre())))
                .toList();
    }

    /** Cache simple: los "top" de IGDB cambian poco y protegen la cuota. */
    private static final long CACHE_OK_MS = 60 * 60 * 1000;
    /** Un fallo/vacío también se recuerda: si IGDB está caído, el endpoint
     *  público no debe pegarle a la API en cada visita anónima. */
    private static final long CACHE_FALLO_MS = 5 * 60 * 1000;
    private volatile List<JuegoExternoResponse> popularesCache = List.of();
    private volatile long popularesCacheMs = 0;
    private volatile boolean popularesCachePoblado = false;

    @Override
    public List<JuegoExternoResponse> populares() {
        if (!disponible()) {
            throw new BusinessException(
                    "El buscador externo no está configurado (faltan TWITCH_CLIENT_ID y TWITCH_CLIENT_SECRET en el backend)");
        }
        long ahora = System.currentTimeMillis();
        long vigencia = popularesCache.isEmpty() ? CACHE_FALLO_MS : CACHE_OK_MS;
        if (popularesCachePoblado && ahora - popularesCacheMs < vigencia) {
            return popularesCache;
        }
        // Ordenado por cantidad de valoraciones: es el proxy de popularidad de
        // IGDB (su "rating" solo, sin volumen, premia juegos de nicho).
        List<JuegoExternoResponse> resultado;
        try {
            resultado = consultar("fields name,slug,cover.image_id,genres.name;"
                    + " where " + SOLO_JUEGOS + " & cover != null & total_rating_count > 50;"
                    + " sort total_rating_count desc;"
                    + " limit 24;")
                    .stream()
                    .filter(juego -> juego.nombre() != null && !juego.nombre().isBlank())
                    .map(IgdbGameSearchServiceImpl::aResumen)
                    .toList();
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

    /**
     * Ficha completa + capturas. A diferencia de RAWG, que necesita dos
     * llamadas, IGDB devuelve los campos anidados en una sola. Nunca tumba el
     * import: si IGDB falla, el juego entra con los datos básicos.
     */
    @Override
    public JuegoExternoDetalle detalle(String slug) {
        if (!disponible() || slug == null || slug.isBlank()) {
            return null;
        }
        String limpio = sanear(slug);
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            IgdbJuego juego = consultar("fields summary,first_release_date,rating,aggregated_rating,"
                    + "websites.url,websites.type,platforms.name,themes.name,screenshots.image_id;"
                    + " where slug = \"" + limpio + "\";"
                    + " limit 1;")
                    .stream().findFirst().orElse(null);
            if (juego == null) {
                return null;
            }
            return new JuegoExternoDetalle(
                    recortar(juego.resumen(), 3900),
                    aFecha(juego.lanzamiento()),
                    aEscalaCinco(juego.valoracion()),
                    aEntero(juego.valoracionCritica()),
                    recortar(sitioOficial(juego.sitios()), 300),
                    juego.plataformas() == null ? List.of() : juego.plataformas().stream()
                            .map(IgdbNombre::name)
                            .filter(n -> n != null && !n.isBlank())
                            .limit(8)
                            .toList(),
                    juego.temas() == null ? List.of() : juego.temas().stream()
                            .map(IgdbNombre::name)
                            .filter(n -> n != null && !n.isBlank())
                            .map(IgdbGameSearchServiceImpl::traducirTema)
                            .limit(8)
                            .toList(),
                    juego.capturas() == null ? List.of() : juego.capturas().stream()
                            // El dashboard consume estas capturas como hero 16:9.
                            .map(imagen -> urlImagen(imagen, "t_screenshot_huge"))
                            .filter(u -> u != null && !u.isBlank())
                            .limit(6)
                            .toList());
        } catch (BusinessException e) {
            return null;
        }
    }

    // ---------- Transporte ----------

    /**
     * POST con el query APICalypse en el body. Ante un 401 (token revocado
     * antes de tiempo) renueva UNA vez y reintenta, igual que HelixClient.
     */
    private List<IgdbJuego> consultar(String query) {
        espaciarLlamadas();
        try {
            try {
                return ejecutar(query, tokenProvider.obtener(false));
            } catch (HttpClientErrorException.Unauthorized e) {
                return ejecutar(query, tokenProvider.obtener(true));
            }
        } catch (RestClientException e) {
            throw new BusinessException("El buscador externo de juegos no está disponible ahora");
        }
    }

    private List<IgdbJuego> ejecutar(String query, String bearer) {
        IgdbJuego[] juegos = restClient.post()
                .uri("/games")
                .header("Client-ID", tokenProvider.clientId())
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.TEXT_PLAIN)
                .body(query)
                .retrieve()
                .body(IgdbJuego[].class);
        return juegos == null ? List.of() : List.of(juegos);
    }

    /** Espera lo que falte para no pasarse de las 4 peticiones por segundo. */
    private void espaciarLlamadas() {
        synchronized (cerrojoRitmo) {
            long esperar = MS_ENTRE_LLAMADAS - (System.currentTimeMillis() - ultimaLlamadaMs);
            if (esperar > 0) {
                try {
                    Thread.sleep(esperar);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ultimaLlamadaMs = System.currentTimeMillis();
        }
    }

    // ---------- Mapeo ----------

    private static JuegoExternoResponse aResumen(IgdbJuego juego) {
        return new JuegoExternoResponse(
                juego.slug(),
                juego.nombre(),
                traducirGenero(juego),
                urlImagen(juego.portada(), "t_cover_big_2x"));
    }

    /**
     * APICalypse no tiene parámetros ligados: el texto se interpola crudo en
     * la consulta, así que una comilla en el nombre ("Baldur's Gate" está bien,
     * pero unas comillas dobles no) la rompería. Se quitan comillas y barras.
     */
    private static String sanear(String texto) {
        return texto == null ? "" : texto.replace("\"", "").replace("\\", "").trim();
    }

    /**
     * IGDB devuelve TODAS las redes del juego en websites (Valorant trae 13) y
     * el orden no significa nada: la primera suele ser YouTube o Discord. El
     * sitio oficial es el de tipo 1; si no lo hay, se cae al primero.
     */
    private static String sitioOficial(List<IgdbSitio> sitios) {
        if (sitios == null || sitios.isEmpty()) {
            return null;
        }
        return sitios.stream()
                .filter(sitio -> Integer.valueOf(1).equals(sitio.tipo()))
                .map(IgdbSitio::url)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElseGet(() -> sitios.get(0).url());
    }

    private static String urlImagen(IgdbImagen imagen, String tamano) {
        if (imagen == null || imagen.imageId() == null || imagen.imageId().isBlank()) {
            return null;
        }
        return IMAGEN_BASE + tamano + "/" + imagen.imageId() + ".jpg";
    }

    /** IGDB entrega la fecha como epoch en segundos. */
    private static LocalDate aFecha(Long epochSegundos) {
        return epochSegundos == null
                ? null
                : Instant.ofEpochSecond(epochSegundos).atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * IGDB puntúa de 0 a 100 y RAWG de 0 a 5. Se convierte acá para que el
     * frontend no tenga que saber de qué proveedor vino cada juego, y para que
     * los ya sembrados con RAWG convivan en la misma escala.
     */
    private static Double aEscalaCinco(Double valoracion) {
        return valoracion == null ? null : Math.round(valoracion / 2.0) / 10.0;
    }

    private static Integer aEntero(Double valor) {
        return valor == null ? null : (int) Math.round(valor);
    }

    private static String recortar(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    /** Primer género traducido; si IGDB no trae géneros, cadena vacía. */
    private static String traducirGenero(IgdbJuego juego) {
        if (juego.generos() == null || juego.generos().isEmpty()) {
            return "";
        }
        String nombre = juego.generos().get(0).name();
        return GENEROS.getOrDefault(nombre, nombre == null ? "" : nombre);
    }

    /**
     * Traduce un tema de IGDB, que en la ficha se muestra como etiqueta.
     *
     * <p>Igual que con los géneros, un tema desconocido se deja tal cual en vez
     * de descartarlo: si IGDB agrega uno nuevo, la etiqueta sale en inglés pero
     * no desaparece de la ficha.</p>
     */
    private static String traducirTema(String nombre) {
        return TEMAS.getOrDefault(nombre, nombre);
    }

    // ---------- Forma mínima de la respuesta de IGDB ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IgdbJuego(@JsonProperty("name") String nombre,
                     String slug,
                     @JsonProperty("cover") IgdbImagen portada,
                     @JsonProperty("genres") List<IgdbNombre> generos,
                     @JsonProperty("summary") String resumen,
                     @JsonProperty("first_release_date") Long lanzamiento,
                     @JsonProperty("rating") Double valoracion,
                     @JsonProperty("aggregated_rating") Double valoracionCritica,
                     @JsonProperty("platforms") List<IgdbNombre> plataformas,
                     @JsonProperty("themes") List<IgdbNombre> temas,
                     @JsonProperty("screenshots") List<IgdbImagen> capturas,
                     @JsonProperty("websites") List<IgdbSitio> sitios) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IgdbImagen(@JsonProperty("image_id") String imageId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IgdbNombre(String name) {
    }

    /** El tipo llega como entero cuando se pide {@code websites.type} plano. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record IgdbSitio(String url, @JsonProperty("type") Integer tipo) {
    }
}
