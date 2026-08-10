package com.coffeecommits.brakket.analytics.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Lote de mensajes de chat a analizar para una transmisión (RF-39).
 *
 * @param mensajes        mensajes de chat capturados (al menos uno no vacío)
 * @param usuariosActivos usuarios distintos que escribieron en la ventana; si
 *                        es null se aproxima con la cantidad de mensajes
 * @param ventanaSegundos duración de la ventana que representa el lote; sirve
 *                        para guardar una tasa por minuto comparable con la del
 *                        muestreo automático (RF-38). Si es null se asume un
 *                        minuto, que es el intervalo por defecto del muestreo
 */
public record AnalizarChatRequest(
        // El tope no es por seguridad (el endpoint es ADMIN) sino por memoria:
        // sin él, pegar miles de líneas entra completo al heap y a una sola
        // transacción. 2000 mensajes cubren de sobra un minuto del chat más
        // movido, y 500 es el largo máximo de un mensaje de Twitch.
        @NotEmpty(message = "Debe enviar al menos un mensaje de chat para analizar")
        @Size(max = MAX_MENSAJES, message = "No se pueden analizar más de {max} mensajes por lote")
        List<@Size(max = MAX_LARGO_MENSAJE,
                message = "Un mensaje de chat no puede superar los {max} caracteres") String> mensajes,

        @Positive(message = "Los usuarios activos deben ser un número positivo")
        @Max(value = MAX_USUARIOS_ACTIVOS, message = "Los usuarios activos no pueden superar {value}")
        Integer usuariosActivos,

        @Positive(message = "La ventana debe expresarse en segundos positivos")
        @Max(value = MAX_VENTANA_SEGUNDOS, message = "La ventana no puede superar {value} segundos")
        Integer ventanaSegundos
) {
    /** Mensajes máximos por lote. */
    public static final int MAX_MENSAJES = 2000;

    /** Largo máximo de un mensaje de chat de Twitch. */
    public static final int MAX_LARGO_MENSAJE = 500;

    public static final int MAX_USUARIOS_ACTIVOS = 1_000_000;

    /** Seis horas: más que cualquier transmisión de una jornada. */
    public static final int MAX_VENTANA_SEGUNDOS = 21_600;

    /** Ventana asumida cuando no se informa: el intervalo por defecto de RF-38. */
    public static final int VENTANA_POR_DEFECTO_SEGUNDOS = 60;
}
