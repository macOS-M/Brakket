package com.coffeecommits.brakket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Parámetros del termómetro de sentimiento (RF-40).
 *
 * Propiedades: brakket.analytics.*
 */
@Component
@ConfigurationProperties(prefix = "brakket.analytics")
public class AnalyticsProperties {

    /**
     * Muestras mínimas para mostrar el indicador. Por debajo, la ERS pide
     * ocultar el termómetro definitivo y avisar que los datos no alcanzan: con
     * una o dos ventanas, una sola jugada polarizante define el color entero.
     */
    private int minimoMuestras = 3;

    /** Ancho del intervalo de la evolución cuando el cliente no lo indica. */
    private int intervaloPorDefectoMinutos = 5;

    /**
     * Tope de intervalos por consulta. Evita que un período largo con un
     * intervalo chico devuelva una respuesta enorme; se responde pidiendo
     * ampliar el intervalo, en vez de recortar en silencio.
     */
    private int maxIntervalos = 500;

    public int getMinimoMuestras() { return minimoMuestras; }
    public void setMinimoMuestras(int minimoMuestras) { this.minimoMuestras = minimoMuestras; }

    public int getIntervaloPorDefectoMinutos() { return intervaloPorDefectoMinutos; }
    public void setIntervaloPorDefectoMinutos(int intervaloPorDefectoMinutos) {
        this.intervaloPorDefectoMinutos = intervaloPorDefectoMinutos;
    }

    public int getMaxIntervalos() { return maxIntervalos; }
    public void setMaxIntervalos(int maxIntervalos) { this.maxIntervalos = maxIntervalos; }
}
