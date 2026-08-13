package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.config.GeminiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Cliente de Gemini contra un servidor HTTP local del JDK, mismo enfoque que
 * {@code AnalizadorIaTest}: se ejerce el camino real —ruta, cabecera, cuerpo,
 * códigos de error— sin salir a la red, que es requisito para que el CI corra
 * sin credenciales.
 *
 * <p>Lo que más importa acá es que el <b>429 se distinga</b> del resto de los
 * fallos: de eso depende poder medir cuánta cuota consume una transmisión larga.</p>
 */
class ClienteGeminiTest {

    private static final String RESPUESTA_OK =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"El pico fue a las 14:20.\"}]}}]}";

    private HttpServer servidor;
    private GeminiProperties properties;
    private ClienteGemini cliente;

    private int llamadas;
    private String rutaRecibida;
    private String cuerpoRecibido;
    private final Map<String, String> cabecerasRecibidas = new HashMap<>();

    private int estadoRespuesta = 200;
    private String cuerpoRespuesta = RESPUESTA_OK;

    @BeforeEach
    void levantarProveedorFalso() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/models", intercambio -> {
            llamadas++;
            rutaRecibida = intercambio.getRequestURI().getPath();
            cuerpoRecibido = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            intercambio.getRequestHeaders().forEach(
                    (clave, valores) -> cabecerasRecibidas.put(clave.toLowerCase(Locale.ROOT), valores.get(0)));

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
        cliente = new ClienteGemini(properties, RestClient.builder());
    }

    @AfterEach
    void bajarProveedorFalso() {
        servidor.stop(0);
    }

    @Test
    void devuelve_el_texto_de_la_respuesta() {
        assertThat(cliente.generar("instrucciones", "pregunta")).isEqualTo("El pico fue a las 14:20.");
    }

    @Test
    void manda_la_llave_en_la_cabecera_y_el_modelo_en_la_ruta() {
        cliente.generar("instrucciones", "pregunta");

        assertThat(cabecerasRecibidas).containsEntry("x-goog-api-key", "llave-de-prueba");
        assertThat(rutaRecibida).isEqualTo("/models/gemini-de-prueba:generateContent");
    }

    @Test
    void manda_las_instrucciones_aparte_de_la_pregunta_y_pide_respuesta_determinista() {
        cliente.generar("sos un asistente", "cuando hubo mas chat");

        assertThat(cuerpoRecibido).contains("system_instruction").contains("sos un asistente");
        assertThat(cuerpoRecibido).contains("cuando hubo mas chat");
        assertThat(cuerpoRecibido).contains("\"temperature\":0");
    }

    @Test
    void pide_json_explicitamente() {
        cliente.generar("instrucciones", "pregunta");

        // Sin esto el proveedor contesta application/octet-stream y el
        // convertidor de Jackson no se aplica: la respuesta llega bien y
        // revienta al leerse.
        assertThat(cabecerasRecibidas.get("accept")).contains("application/json");
    }

    @Test
    void acota_el_razonamiento_del_modelo() {
        cliente.generar("instrucciones", "pregunta");

        // El razonamiento se descuenta del tope de salida y se paga: sin
        // acotarlo, pedir un numero gastaba cientos de tokens pensando.
        assertThat(cuerpoRecibido).contains("thinkingLevel").contains("low");
    }

    @Test
    void omite_el_razonamiento_cuando_no_esta_configurado() {
        properties.setThinkingLevel("");

        cliente.generar("instrucciones", "pregunta");

        // Hay modelos que rechazan el campo con 400; dejarlo vacio permite
        // usarlos sin tocar codigo.
        assertThat(cuerpoRecibido).doesNotContain("thinkingConfig");
    }

    @Test
    void avisa_cuando_la_respuesta_se_corta_por_falta_de_tokens() {
        cuerpoRespuesta = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"punt\"}]},"
                + "\"finishReason\":\"MAX_TOKENS\"}]}";

        // El sintoma es un texto ilegible; el mensaje tiene que delatar la causa.
        assertThatThrownBy(() -> cliente.generar("instrucciones", "pregunta"))
                .isInstanceOf(IaNoDisponibleException.class)
                .hasMessageContaining("tokens");
    }

    @Test
    void marca_el_limite_de_tasa_cuando_el_proveedor_responde_429() {
        estadoRespuesta = 429;
        cuerpoRespuesta = "{\"error\":{\"message\":\"quota exceeded\"}}";

        IaNoDisponibleException error = catchThrowableOfType(
                () -> cliente.generar("instrucciones", "pregunta"), IaNoDisponibleException.class);

        assertThat(error).isNotNull();
        assertThat(error.isPorLimiteDeTasa()).isTrue();
    }

    @Test
    void no_marca_limite_de_tasa_ante_otros_errores_del_proveedor() {
        estadoRespuesta = 500;
        cuerpoRespuesta = "{\"error\":{\"message\":\"boom\"}}";

        IaNoDisponibleException error = catchThrowableOfType(
                () -> cliente.generar("instrucciones", "pregunta"), IaNoDisponibleException.class);

        assertThat(error).isNotNull();
        assertThat(error.isPorLimiteDeTasa()).isFalse();
    }

    @Test
    void falla_cuando_el_proveedor_bloquea_el_pedido() {
        cuerpoRespuesta = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";

        assertThatThrownBy(() -> cliente.generar("instrucciones", "pregunta"))
                .isInstanceOf(IaNoDisponibleException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    void falla_cuando_la_respuesta_no_trae_texto() {
        cuerpoRespuesta = "{\"candidates\":[]}";

        assertThatThrownBy(() -> cliente.generar("instrucciones", "pregunta"))
                .isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void sin_llave_no_sale_a_la_red() {
        properties.setApiKey("");

        assertThat(cliente.estaConfigurado()).isFalse();
        assertThatThrownBy(() -> cliente.generar("instrucciones", "pregunta"))
                .isInstanceOf(IaNoDisponibleException.class);
        assertThat(llamadas).isZero();
    }
}
