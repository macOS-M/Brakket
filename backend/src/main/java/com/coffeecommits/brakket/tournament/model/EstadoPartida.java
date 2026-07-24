package com.coffeecommits.brakket.tournament.model;

/**
 * Ciclo de vida de una partida del bracket. El resultado lo reporta un
 * capitán y lo confirma el rival; en desacuerdo queda EN_DISPUTA y la
 * resuelve el organizador (o un ADMIN).
 */
public enum EstadoPartida {
    /** Esperando resultado (o esperando rivales de rondas previas). */
    PENDIENTE,
    /** Un capitán reportó el marcador; falta la confirmación del rival. */
    REPORTADA,
    /** El rival rechazó el reporte; decide el organizador. */
    EN_DISPUTA,
    /** Resultado confirmado; el ganador ya avanzó en la llave. */
    FINALIZADA,
    /** Anulada (torneo cancelado); no bloquea disoluciones de equipo. */
    CANCELADA
}
