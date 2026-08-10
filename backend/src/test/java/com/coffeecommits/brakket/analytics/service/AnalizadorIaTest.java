package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import com.coffeecommits.brakket.config.AiProperties;
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
    private AiProperties properties;

    private int llamadas;
    private String cuerpoRecibido;
    private final Map<String, String> cabecerasRecibidas = new HashMap<>();

    private int estadoRespuesta = 200;
    private String cuerpoRespuesta = "{}";

    @BeforeEach
    void levantarProveedorFalso() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/messages", intercambio -> {
            llamadas++;
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

        properties = new AiProperties();
        properties.setApiKey("llave-de-prueba");
        properties.setApiBaseUrl("http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void bajarProveedorFalso() {
        servidor.stop(0);
    }

    private AnalizadorIa analizador() {
        return new AnalizadorIa(properties, RestClient.builder(), MAPPER, new AnalizadorLexico());
    }

    /** Envuelve un texto en la forma que devuelve la API de mensajes. */
    private void elModeloResponde(String texto) throws Exception {
        cuerpoRespuesta = "{\"content\":[{\"type\":\"text\",\"text\":"
                + MAPPER.writeValueAsString(texto) + "}]}";
    }

    /** Contenido que viajó al modelo, una línea por mensaje. */
    private List<String> lineasEnviadas() throws Exception {
        String contenido = MAPPER.readTree(cuerpoRecibido)
                .path("messages").get(0).path("content").asText();
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
    void manda_la_llave_y_la_version_de_la_api_en_cabeceras() throws Exception {
        elModeloResponde("{\"puntaje\": 0}");

        analizador().analizar(MENSAJES);

        assertThat(cabecerasRecibidas).containsEntry("x-api-key", "llave-de-prueba");
        assertThat(cabecerasRecibidas).containsEntry("anthropic-version", properties.getApiVersion());
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
            contexto.register(AiProperties.class, AnalizadorLexico.class, AnalizadorIa.class);
            contexto.refresh();

            assertThat(contexto.getBean(AnalizadorSentimiento.class)).isInstanceOf(AnalizadorIa.class);
        }
    }
}
