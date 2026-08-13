package com.coffeecommits.brakket.analytics.dto;

/**
 * Resultado de pedir una clasificación de sentimiento fuera de cadencia
 * (RF-39, EPIC-10).
 *
 * @param clasificado false cuando no había chat acumulado todavía
 * @param mensajes    cuántos mensajes se mandaron al analizador
 * @param mensaje     texto para mostrarle al administrador
 */
public record ClasificacionInmediataResponse(
        boolean clasificado,
        int mensajes,
        String mensaje) {

    public static ClasificacionInmediataResponse sinChat() {
        return new ClasificacionInmediataResponse(false, 0,
                "Todavía no hay chat acumulado para analizar. Esperá a que el muestreo capture una ventana.");
    }

    public static ClasificacionInmediataResponse de(int mensajes) {
        return new ClasificacionInmediataResponse(true, mensajes,
                "Se analizó un bloque de " + mensajes + " mensajes. Actualizá el termómetro para verlo.");
    }
}
