package com.coffeecommits.brakket.tournament.model;

import jakarta.validation.constraints.Size;

/**
 * Regla estructurada de partida definida por el organizador (referencia:
 * "Game settings" de Challenger Mode). Brakket no la aplica dentro del
 * juego: la publica como contrato que ambos capitanes deben configurar al
 * crear la lobby, y sirve de base para resolver disputas.
 */
public record AjustePartida(
        @Size(max = 60, message = "El nombre de un ajuste no puede superar los 60 caracteres")
        String clave,
        @Size(max = 200, message = "El valor de un ajuste no puede superar los 200 caracteres")
        String valor
) {
}
