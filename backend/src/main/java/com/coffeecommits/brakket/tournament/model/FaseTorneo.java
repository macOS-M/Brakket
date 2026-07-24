package com.coffeecommits.brakket.tournament.model;

/**
 * Sección de la competencia a la que pertenece una partida. Null en los
 * formatos de una sola estructura (eliminación directa, round robin, suizo).
 */
public enum FaseTorneo {
    /** Llave superior de la doble eliminación. */
    WINNERS,
    /** Llave inferior: ahí caen los perdedores de la superior. */
    LOSERS,
    /** Cruce final de la doble eliminación (sin bracket reset). */
    GRAN_FINAL,
    /** Round robin dentro de un grupo (formato fase de grupos). */
    GRUPOS,
    /** Llave eliminatoria que sigue a la fase de grupos. */
    ELIMINACION
}
