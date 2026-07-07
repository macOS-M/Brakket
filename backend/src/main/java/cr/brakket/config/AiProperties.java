package cr.brakket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del servicio de Inteligencia Artificial para el análisis de
 * sentimiento del chat (EPIC-10, RF-39). Encapsulada para cumplir RNF-23.
 *
 * Propiedades: brakket.ai.*
 */
@Component
@ConfigurationProperties(prefix = "brakket.ai")
public class AiProperties {

    /** API key del proveedor de IA (variable de entorno AI_API_KEY). */
    private String apiKey;

    /** Base URL del servicio de IA. */
    private String apiBaseUrl;

    /** Modelo a utilizar para clasificar sentimiento. */
    private String model = "sentiment-default";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
