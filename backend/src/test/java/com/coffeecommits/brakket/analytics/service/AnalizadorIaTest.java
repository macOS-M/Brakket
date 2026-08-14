package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import com.coffeecommits.brakket.config.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestClient;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-39 con IA. El proveedor se sustituye por un servidor HTTP local del JDK en
 * un puerto libre: se ejerce el camino real (cabeceras, cuerpo, códigos de
 * error) sin salir a la red, que es requisito para que el CI corra sin llave.
 *
 * <p>Lo que se protege acá es sobre todo la <b>degradación</b>: el análisis
 * cuelga del muestreo automático de RF-38, así que ningún fallo del proveedor
 * puede dejar a la transmisión sin serie.</p>
 */
class AnalizadorIaTest {

    /** Puntaje que devuelve el léxico para {@link #MENSAJES}: 2 positivas, 1 negativa. */
    private static final String PUNTAJE_LEXICO = "33.33";
    private static final List<String> MENSAJES = List.of("gg", "gg", "lag");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer servidor;
    private GeminiProperties properties;

    private int llamadas;
    private String rutaRecibida;
    private String cuerpoRecibido;
    private final Map<String, String> cabecerasRecibidas = new HashMap<>();

    private int estadoRespuesta = 200;
    private String cuerpoRespuesta = "{}";

    @BeforeEach
    void levantarProveedorFalso() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/models", intercambio -> {
            llamadas++;
            rutaRecibida = intercambio.getRequestURI().getPath();
            cuerpoRecibido = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            intercambio.getRequestHeaders()
                    .forEach((clave, valores) -> cabecerasRecibidas.put(clave.toLowerCase(Locale.ROOT), valores.get(0)));

            byte[] cuerpo = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estadoRespuesta, cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();

        properties = new GeminiProperties();
        properties.setApiKey("llave-de-prueba");
        properties.setModel("gemini-de-prueba");
        properties.setBaseUrl("http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void bajarProveedorFalso() {
        servidor.stop(0);
    }

    private AnalizadorIa analizador() {
        return new AnalizadorIa(new ClienteGemini(properties, RestClient.builder()),
                properties, MAPPER, new AnalizadorLexico());
    }

    /** Envuelve un texto en la forma que devuelve la API de Gemini. */
    private void elModeloResponde(String texto) throws Exception {
        cuerpoRespuesta = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + MAPPER.writeValueAsString(texto) + "}]}}]}";
    }

    /** Contenido que viajó al modelo, una línea por mensaje. */
    private List<String> lineasEnviadas() throws Exception {
        String contenido = MAPPER.readTree(cuerpoRecibido)
                .path("contents").get(0).path("parts").get(0).path("text").asText();
        return List.of(contenido.split("\n"));
    }

    @Test
    void sin_llave_configurada_no_sale_a_la_red() {
        properties.setApiKey("   ");

        AnalizadorSentimiento.Resultado resultado = analizador().analizar(MENSAJES);

        // Camino de la demo y del CI: ni siquiera se intenta la llamada.
        assertThat(llamadas).isZero();
        assertThat(resultado.puntaje()).isEqualByComparingTo(PUNTAJE_LEXICO);
    }

    @Test
    void usa_el_puntaje_que_devuelve_el_modelo() throws Exception {
        elModeloResponde("{\"puntaje\": 55}");

        AnalizadorSentimiento.Resultado resultado = analizador().analizar(MENSAJES);

        assertThat(llamadas).isEqualTo(1);
        assertThat(resultado.puntaje()).isEqualByComparingTo("55.00");
        assertThat(resultado.clasificacion()).isEqualTo(ClasificacionSentimiento.POSITIVO);
    }

    @Test
    void deriva_la_clasificacion_con_el_mismo_umbral_que_el_lexico() throws Exception {
        // 19.99 se queda a un centesimo del umbral de 20: debe ser NEUTRO, igual
        // que lo seria en el lexico. Las dos series comparten termometro (RF-40).
        elModeloResponde("{\"puntaje\": 19.99}");

        assertThat(analizador().analizar(MENSAJES).clasificacion())
                .isEqualTo(ClasificacionSentimiento.NEUTRO);
    }

    @Test
    void manda_la_llave_en_la_cabecera_y_el_modelo_en_la_ruta() throws Exception {
        elModeloResponde("{\"puntaje\": 0}");

        analizador().analizar(MENSAJES);

        assertThat(cabecerasRecibidas).containsEntry("x-goog-api-key", "llave-de-prueba");
        assertThat(rutaRecibida).isEqualTo("/models/gemini-de-prueba:generateContent");
    }

    @Test
    void pide_un_presupuesto_corto_de_respuesta() throws Exception {
        elModeloResponde("{\"puntaje\": 0}");

        analizador().analizar(MENSAJES);

        // Es la llamada mas frecuente del sistema (una por ventana y por
        // transmision abierta) y la respuesta es un JSON de una linea. El tope
        // igual tiene que cubrir el razonamiento, que se descuenta del mismo
        // presupuesto: con 128 la respuesta llegaba truncada.
        assertThat(MAPPER.readTree(cuerpoRecibido).path("generationConfig").path("maxOutputTokens").asInt())
                .isEqualTo(256);
    }

    @Test
    void un_error_del_proveedor_cae_al_lexico() throws Exception {
        estadoRespuesta = 500;
        cuerpoRespuesta = "{\"error\":\"overloaded\"}";

        AnalizadorSentimiento.Resultado resultado = analizador().analizar(MENSAJES);

        assertThat(llamadas).isEqualTo(1);
        // La ventana igual queda clasificada: RF-38 no puede quedarse sin serie.
        assertThat(resultado.puntaje()).isEqualByComparingTo(PUNTAJE_LEXICO);
    }

    @Test
    void agotar_la_cuota_cae_al_lexico_sin_tumbar_la_ventana() {
        estadoRespuesta = 429;
        cuerpoRespuesta = "{\"error\":{\"message\":\"quota exceeded\"}}";

        AnalizadorSentimiento.Resultado resultado = analizador().analizar(MENSAJES);

        // El limite del tier gratuito es esperable en transmisiones largas; la
        // serie sigue completa, solo que esa ventana la puntua el lexico.
        assertThat(resultado.puntaje()).isEqualByComparingTo(PUNTAJE_LEXICO);
    }

    @Test
    void una_respuesta_que_no_es_json_cae_al_lexico() throws Exception {
        elModeloResponde("Claro, el chat se ve bastante contento.");

        assertThat(analizador().analizar(MENSAJES).puntaje())
                .isEqualByComparingTo(PUNTAJE_LEXICO);
    }

    @Test
    void un_json_sin_puntaje_numerico_cae_al_lexico() throws Exception {
        elModeloResponde("{\"puntaje\": \"muy positivo\"}");

        assertThat(analizador().analizar(MENSAJES).puntaje())
                .isEqualByComparingTo(PUNTAJE_LEXICO);
    }

    @Test
    void tolera_que_el_modelo_envuelva_el_json_en_vallas_de_markdown() throws Exception {
        elModeloResponde("```json\n{\"puntaje\": -40}\n```");

        AnalizadorSentimiento.Resultado resultado = analizador().analizar(MENSAJES);

        assertThat(resultado.puntaje()).isEqualByComparingTo("-40.00");
        assertThat(resultado.clasificacion()).isEqualTo(ClasificacionSentimiento.NEGATIVO);
    }

    @Test
    void acota_un_puntaje_fuera_de_la_escala() throws Exception {
        elModeloResponde("{\"puntaje\": 250}");

        assertThat(analizador().analizar(MENSAJES).puntaje())
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void manda_solo_los_mensajes_mas_recientes_de_la_ventana() throws Exception {
        properties.setMaxMensajes(3);
        elModeloResponde("{\"puntaje\": 0}");

        analizador().analizar(List.of("m0", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9"));

        // Los ultimos describen el clima actual; mandar la ventana entera solo
        // gastaria tokens.
        assertThat(lineasEnviadas()).containsExactly("m7", "m8", "m9");
    }

    @Test
    void trunca_los_mensajes_muy_largos() throws Exception {
        properties.setMaxLargoMensaje(5);
        elModeloResponde("{\"puntaje\": 0}");

        analizador().analizar(List.of("abcdefghij"));

        assertThat(lineasEnviadas()).containsExactly("abcde");
    }

    /**
     * Desde que hay dos implementaciones de {@link AnalizadorSentimiento},
     * inyectar la interfaz es ambiguo y la app no arranca si falta el
     * {@code @Primary}. El {@code contextLoads} lo detectaria, pero solo con una
     * base de datos levantada; este contexto minimo lo verifica sin BD.
     */
    @Test
    void la_ia_es_el_analizador_que_se_inyecta_por_defecto() {
        try (AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext()) {
            contexto.registerBean(ObjectMapper.class);
            // Lambda y no RestClient::builder: la referencia a metodo es
            // ambigua contra la sobrecarga que recibe un RestTemplate.
            contexto.registerBean(RestClient.Builder.class, () -> RestClient.builder());
            contexto.register(GeminiProperties.class, ClienteGemini.class,
                    AnalizadorLexico.class, AnalizadorIa.class);
            contexto.refresh();

            assertThat(contexto.getBean(AnalizadorSentimiento.class)).isInstanceOf(AnalizadorIa.class);
        }
    }
}
