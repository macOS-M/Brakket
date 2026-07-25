package com.coffeecommits.brakket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la página de transmisiones (RF-35).
 *
 * Propiedades: brakket.streams.*
 */
@Component
@ConfigurationProperties(prefix = "brakket.streams")
public class StreamsProperties {

    /**
     * Feature flag: con false (default) la página solo muestra el canal
     * oficial de Brakket; con true incorpora las transmisiones registradas
     * en transmision_twitch. El código recorre una lista en ambos casos.
     */
    private boolean multiSourceEnabled = false;

    /**
     * TTL del caché de respuestas de Helix. 25s + polling de 30s en el front
     * dan un peor caso de ~55s de desfase: dentro del minuto de RNF-02.
     */
    private int cacheTtlSegundos = 25;

    /** El VOD de un canal offline cambia poco; se consulta con otra cadencia. */
    private int vodCacheSegundos = 600;

    public boolean isMultiSourceEnabled() { return multiSourceEnabled; }
    public void setMultiSourceEnabled(boolean multiSourceEnabled) { this.multiSourceEnabled = multiSourceEnabled; }

    public int getCacheTtlSegundos() { return cacheTtlSegundos; }
    public void setCacheTtlSegundos(int cacheTtlSegundos) { this.cacheTtlSegundos = cacheTtlSegundos; }

    public int getVodCacheSegundos() { return vodCacheSegundos; }
    public void setVodCacheSegundos(int vodCacheSegundos) { this.vodCacheSegundos = vodCacheSegundos; }
}
