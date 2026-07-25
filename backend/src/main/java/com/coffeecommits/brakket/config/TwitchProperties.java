package com.coffeecommits.brakket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la integración con la API de Twitch (EPIC-10).
 *
 * <p>Encapsula credenciales y endpoints para cumplir RNF-23 (mantenibilidad de
 * integraciones externas): cambiar de llaves o proveedor no debe tocar la lógica
 * de torneos. Los valores vienen de variables de entorno (ver .env.example).</p>
 *
 * Propiedades: brakket.twitch.*
 */
@Component
@ConfigurationProperties(prefix = "brakket.twitch")
public class TwitchProperties {

    /** Client ID de la aplicación registrada en Twitch. */
    private String clientId;

    /** Client Secret de la aplicación de Twitch. */
    private String clientSecret;

    /** Canal oficial de Brakket para transmisiones (RF-34). */
    private String channel;

    /** Base URL de la API Helix de Twitch. */
    private String apiBaseUrl = "https://api.twitch.tv/helix";
    private String authBaseUrl = "https://id.twitch.tv/oauth2";

    /**
     * Con true (default) la app NO ARRANCA si faltan las credenciales: un
     * despliegue al que se le olvidó una variable debe fallar al inicio con un
     * mensaje claro, no en runtime frente al usuario. Solo test/CI lo apagan.
     */
    private boolean required = true;

    /**
     * Timeouts cortos: las llamadas a Twitch corren dentro del request; sin
     * límite, un Twitch colgado retiene el hilo de Tomcat indefinidamente.
     */
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getAuthBaseUrl() { return authBaseUrl; }
    public void setAuthBaseUrl(String authBaseUrl) { this.authBaseUrl = authBaseUrl; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
