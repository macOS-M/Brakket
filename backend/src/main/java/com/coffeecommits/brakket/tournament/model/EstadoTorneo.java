package com.coffeecommits.brakket.tournament.model;

/** Estados validos del ciclo de vida de un torneo. */
public enum EstadoTorneo {
    BORRADOR,
    INSCRIPCION_ABIERTA,
    PROGRAMADO,
    EN_CURSO,
    FINALIZADO,
    CANCELADO;

    public boolean estaCerrado() {
        return this == FINALIZADO || this == CANCELADO;
    }
}
