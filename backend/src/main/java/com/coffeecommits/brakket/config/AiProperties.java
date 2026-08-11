package com.coffeecommits.brakket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del servicio de IA que clasifica el sentimiento del chat
 * (RF-39, EPIC-10).
 *
 * <p>A diferencia de {@link TwitchProperties}, aquí <b>no</b> hay fail-fast: sin
 * llave el análisis no se cae, se degrada al analizador léxico. RF-39 corre
 * detrás del muestreo automático de RF-38, y dejar la app sin arrancar —o peor,
 * perder muestras— por una llave ausente sería un remedio peor que la
 * enfermedad. La demo y el CI funcionan sin credenciales.</p>
 *
 * Propiedades: brakket.ai.*
 */
@Component
@ConfigurationProperties(prefix = "brakket.ai")
public class AiProperties {

    /** Llave del proveedor. Vacía = se usa el analizador léxico. */
    private String apiKey;

    /** Base de la API de mensajes del proveedor. */
    private String apiBaseUrl = "https://api.anthropic.com/v1";

    /** Modelo a consultar. */
    private String model = "claude-sonnet-5";

    /** Valor de la cabecera {@code anthropic-version}. */
    private String apiVersion = "2023-06-01";

    /**
     * Tope de mensajes que se envían al modelo por ventana. El DTO admite hasta
     * 2000, pero mandarlos todos gasta tokens sin mejorar la lectura agregada:
     * se toman los más recientes, que son los que describen el clima actual.
     */
    private int maxMensajes = 200;

    /** Tope de caracteres por mensaje enviado al modelo. */
    private int maxLargoMensaje = 280;

    /**
     * Timeouts. El análisis corre en el hilo del planificador después del
     * commit del muestreo; sin límite, un proveedor colgado se comería el tick
     * siguiente. Lectura más holgada que en Twitch porque un modelo tarda más
     * que una consulta REST.
     */
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 10000;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public int getMaxMensajes() { return maxMensajes; }
    public void setMaxMensajes(int maxMensajes) { this.maxMensajes = maxMensajes; }

    public int getMaxLargoMensaje() { return maxLargoMensaje; }
    public void setMaxLargoMensaje(int maxLargoMensaje) { this.maxLargoMensaje = maxLargoMensaje; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    /** Sin llave el analizador de IA ni siquiera intenta la llamada. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
