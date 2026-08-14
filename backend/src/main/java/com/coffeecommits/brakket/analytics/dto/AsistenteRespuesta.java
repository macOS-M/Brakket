package com.coffeecommits.brakket.analytics.dto;

/**
 * Respuesta del asistente del termómetro (RF-40, EPIC-10).
 *
 * <p>{@code generadaPorIa} y {@code aviso} viajan a propósito hasta la vista: si
 * el asistente degradó al camino determinista, el administrador tiene que
 * saberlo. Una respuesta armada con plantillas presentada como si la hubiera
 * redactado el modelo sería engañosa, y además esconde el dato que hace falta
 * para medir cuánto de una transmisión larga se quedó sin cuota.</p>
 *
 * @param respuesta     texto para mostrar; nunca viene vacío
 * @param generadaPorIa false cuando respondió el camino determinista
 * @param aviso         motivo de la degradación, o null si respondió la IA
 */
public record AsistenteRespuesta(
        String respuesta,
        boolean generadaPorIa,
        String aviso) {

    public static AsistenteRespuesta deIa(String respuesta) {
        return new AsistenteRespuesta(respuesta, true, null);
    }

    public static AsistenteRespuesta degradada(String respuesta, String aviso) {
        return new AsistenteRespuesta(respuesta, false, aviso);
    }
}
