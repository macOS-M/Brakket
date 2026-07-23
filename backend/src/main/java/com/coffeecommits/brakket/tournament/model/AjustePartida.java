package com.coffeecommits.brakket.tournament.model;

/**
 * Regla estructurada de partida definida por el organizador (referencia:
 * "Game settings" de Challenger Mode). Brakket no la aplica dentro del
 * juego: la publica como contrato que ambos capitanes deben configurar al
 * crear la lobby, y sirve de base para resolver disputas.
 */
public record AjustePartida(String clave, String valor) {
}
