package com.coffeecommits.brakket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del proveedor de IA generativa (Google Gemini).
 *
 * <p>Se eligió Gemini sobre Anthropic por el tier gratuito: el proyecto no tiene
 * presupuesto y la API de Google no exige tarjeta. El costo del cambio es el
 * mapeo del request/response, que vive aislado en {@code ClienteGemini}.</p>
 *
 * <p>A diferencia de {@link TwitchProperties}, <b>no</b> hay fail-fast: sin
 * llave nada se cae. El asistente responde con los números que ya calculó el
 * backend y el análisis de sentimiento degrada al analizador léxico. Dejar la
 * app sin arrancar por una credencial ausente sería un remedio peor que la
 * enfermedad; la demo y el CI corren así a propósito.</p>
 *
 * Propiedades: brakket.ai.gemini.*
 */
@Component
@ConfigurationProperties(prefix = "brakket.ai.gemini")
public class GeminiProperties {

    /** Llave de Google AI Studio. Vacía = el asistente responde sin IA. */
    private String apiKey;

    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    /**
     * Modelo a consultar. Se usa el alias {@code -latest} y no una versión fija
     * a propósito: Google jubila las versiones concretas y devuelve 404 aunque
     * sigan apareciendo en el listado de modelos ({@code gemini-2.5-flash} dejó
     * de estar disponible para llaves nuevas). El alias sobrevive a ese cambio.
     * Si alguna vez hace falta fijar una versión, {@code gemini-3.6-flash} está
     * verificada contra esta cuenta.
     */
    private String model = "gemini-flash-latest";

    /**
     * Tope de tokens de la respuesta. El asistente redacta un párrafo o dos
     * citando timestamps; más presupuesto solo alarga la espera. El análisis de
     * sentimiento pide bastante menos, y lo pasa por parámetro.
     *
     * <p>Ojo: los tokens de razonamiento del modelo <b>cuentan contra este
     * tope</b>. Si queda corto, la respuesta llega truncada con
     * {@code finishReason: MAX_TOKENS} y no se puede parsear.</p>
     */
    private int maxTokens = 900;

    /**
     * Profundidad de razonamiento del modelo. Los Gemini 3.x piensan por
     * defecto y eso se paga dos veces: consume cuota y se descuenta de
     * {@link #maxTokens}. Sin acotarlo, clasificar una ventana de chat gastaba
     * 338 tokens de razonamiento para devolver un número.
     *
     * <p>Vacío = no se manda el campo, para modelos que no lo aceptan
     * ({@code thinkingBudget} de los 2.5 ya no es válido acá).</p>
     */
    private String thinkingLevel = "low";

    /**
     * Tope de mensajes de chat que se envían por ventana al clasificar el
     * sentimiento (RF-39). Una ventana en pico puede traer miles de líneas y la
     * lectura agregada no mejora por leerlas todas: se toman las más recientes,
     * que son las que describen el clima actual.
     */
    private int maxMensajes = 200;

    /** Tope de caracteres por mensaje enviado al modelo. */
    private int maxLargoMensaje = 280;

    /**
     * Timeouts. La consulta corre en el hilo de la petición HTTP del admin, así
     * que un proveedor colgado se traduce en una pantalla congelada. Lectura
     * holgada porque un modelo tarda más que una consulta REST normal.
     */
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 20000;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public String getThinkingLevel() { return thinkingLevel; }
    public void setThinkingLevel(String thinkingLevel) { this.thinkingLevel = thinkingLevel; }

    public int getMaxMensajes() { return maxMensajes; }
    public void setMaxMensajes(int maxMensajes) { this.maxMensajes = maxMensajes; }

    public int getMaxLargoMensaje() { return maxLargoMensaje; }
    public void setMaxLargoMensaje(int maxLargoMensaje) { this.maxLargoMensaje = maxLargoMensaje; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    /** Sin llave el cliente ni siquiera intenta la llamada. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
