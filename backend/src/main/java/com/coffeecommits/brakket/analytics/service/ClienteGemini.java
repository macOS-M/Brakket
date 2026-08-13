package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API de Gemini (Google AI Studio).
 *
 * <p>Mismo patrón que {@code HelixClient} y {@code AnalizadorIa}: RestClient
 * inyectado, timeouts propios y credencial en cabecera. No se sumó el SDK de
 * Google a propósito, por lo mismo que no se sumó el de Anthropic: una
 * dependencia nueva para una única llamada REST pesa más en el build y en el CI
 * que el código que ahorra.</p>
 *
 * <p>Es deliberadamente tonto: arma el cuerpo, lee el texto y traduce los fallos
 * a {@link IaNoDisponibleException}. Quién degrada y hacia qué es decisión de
 * cada servicio que lo use, no del cliente.</p>
 */
@Component
@Slf4j
public class ClienteGemini {

    private final GeminiProperties properties;
    private final RestClient.Builder restClientBuilder;

    public ClienteGemini(GeminiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    /** false cuando no hay llave: permite decidir sin pagar el viaje a la red. */
    public boolean estaConfigurado() {
        return properties.isConfigured();
    }

    /**
     * Consulta el modelo y devuelve el texto de la respuesta.
     *
     * @param instrucciones system prompt; define el rol y las reglas
     * @param entrada       contenido del turno del usuario
     * @throws IaNoDisponibleException ante cualquier fallo, con el motivo distinguido
     */
    public String generar(String instrucciones, String entrada) {
        return generar(instrucciones, entrada, properties.getMaxTokens());
    }

    /**
     * Variante con presupuesto propio de respuesta. La usa el análisis de
     * sentimiento, que espera un JSON de una línea y corre una vez por minuto
     * por transmisión: darle el presupuesto del asistente sería pagar de más en
     * la llamada más frecuente del sistema.
     */
    public String generar(String instrucciones, String entrada, int maxTokens) {
        if (!estaConfigurado()) {
            throw new IaNoDisponibleException("no hay llave de Gemini configurada", false);
        }
        return textoDeLaRespuesta(pedir(instrucciones, entrada, maxTokens));
    }

    private JsonNode pedir(String instrucciones, String entrada, int maxTokens) {
        try {
            return restClientBuilder.baseUrl(properties.getBaseUrl())
                    .requestFactory(fabricaConTimeouts()).build()
                    .post().uri("/models/{model}:generateContent", properties.getModel())
                    .header("x-goog-api-key", properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    // Sin Accept explícito el proveedor contesta
                    // application/octet-stream y el convertidor de Jackson no
                    // se aplica: la respuesta llega bien y falla al leerse.
                    .accept(MediaType.APPLICATION_JSON)
                    .body(cuerpo(instrucciones, entrada, maxTokens))
                    .retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            // El tier gratuito limita por minuto y por día. Se marca aparte para
            // poder medir cuánto de una transmisión larga se quedó sin IA.
            throw new IaNoDisponibleException("limite de tasa del proveedor", true);
        } catch (RestClientException ex) {
            throw new IaNoDisponibleException(ex.getMessage(), ex);
        }
    }

    private Map<String, Object> cuerpo(String instrucciones, String entrada, int maxTokens) {
        Map<String, Object> generacion = new LinkedHashMap<>();
        // Determinista: la misma pregunta sobre los mismos datos debe dar la
        // misma respuesta, o la demo sería irreproducible.
        generacion.put("temperature", 0);
        generacion.put("maxOutputTokens", maxTokens);
        if (properties.getThinkingLevel() != null && !properties.getThinkingLevel().isBlank()) {
            generacion.put("thinkingConfig", Map.of("thinkingLevel", properties.getThinkingLevel()));
        }
        return Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", instrucciones))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", entrada)))),
                "generationConfig", generacion);
    }

    /**
     * Concatena los bloques de texto del primer candidato. Se revisa antes si el
     * filtro de seguridad bloqueó el pedido: en ese caso la respuesta llega sin
     * candidatos y sin el bloqueo se leería como "el modelo no contestó".
     */
    private String textoDeLaRespuesta(JsonNode respuesta) {
        if (respuesta == null) {
            throw new IaNoDisponibleException("respuesta vacia del proveedor", false);
        }
        JsonNode bloqueo = respuesta.path("promptFeedback").path("blockReason");
        if (!bloqueo.isMissingNode() && !bloqueo.asText().isBlank()) {
            throw new IaNoDisponibleException("el proveedor bloqueo el pedido: " + bloqueo.asText(), false);
        }

        JsonNode candidato = respuesta.path("candidates").path(0);
        StringBuilder texto = new StringBuilder();
        for (JsonNode parte : candidato.path("content").path("parts")) {
            String fragmento = parte.path("text").asText("");
            if (!fragmento.isBlank()) {
                texto.append(fragmento);
            }
        }
        // Los tokens de razonamiento se descuentan del tope de salida, así que
        // un presupuesto corto devuelve texto cortado a la mitad. Se nombra
        // aparte porque el síntoma —un JSON ilegible— no delata la causa.
        if ("MAX_TOKENS".equals(candidato.path("finishReason").asText())) {
            throw new IaNoDisponibleException(
                    "la respuesta se corto por falta de tokens (subir max-tokens o bajar thinking-level)", false);
        }
        if (texto.isEmpty()) {
            throw new IaNoDisponibleException("la respuesta no trae texto", false);
        }
        return texto.toString().trim();
    }

    private SimpleClientHttpRequestFactory fabricaConTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return factory;
    }
}
