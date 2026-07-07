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

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
}
