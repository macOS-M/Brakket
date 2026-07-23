package com.coffeecommits.brakket.tournament.dto;

import com.coffeecommits.brakket.tournament.model.Partida;

/**
 * Partida del bracket aplanada para el frontend. El nombre de la lobby es
 * público (identifica el cruce), pero la clave solo viaja a quien puede
 * entrar a la partida privada: capitanes de ese cruce, organizador o ADMIN.
 */
public record PartidaResponse(
        Long id,
        Integer ronda,
        Integer orden,
        Long equipoAId,
        String equipoANombre,
        String equipoALogo,
        Long equipoBId,
        String equipoBNombre,
        String equipoBLogo,
        Integer marcadorA,
        Integer marcadorB,
        Long ganadorEquipoId,
        Long reportadoPorEquipoId,
        String estado,
        boolean bye,
        String lobbyNombre,
        String lobbyClave,
        Long siguientePartidaId
) {
    /** Vista completa: para quien actúa sobre la partida (capitán/gestor). */
    public static PartidaResponse from(Partida p) {
        return from(p, true);
    }

    public static PartidaResponse from(Partida p, boolean incluirClave) {
        return new PartidaResponse(
                p.getId(),
                p.getRonda(),
                p.getOrden(),
                p.getEquipoA() == null ? null : p.getEquipoA().getId(),
                p.getEquipoA() == null ? null : p.getEquipoA().getNombre(),
                p.getEquipoA() == null ? null : p.getEquipoA().getLogo(),
                p.getEquipoB() == null ? null : p.getEquipoB().getId(),
                p.getEquipoB() == null ? null : p.getEquipoB().getNombre(),
                p.getEquipoB() == null ? null : p.getEquipoB().getLogo(),
                p.getMarcadorA(),
                p.getMarcadorB(),
                p.getGanador() == null ? null : p.getGanador().getId(),
                p.getReportadoPor() == null ? null : p.getReportadoPor().getId(),
                p.getEstado() == null ? null : p.getEstado().name(),
                p.esBye(),
                p.getLobbyNombre(),
                incluirClave ? p.getLobbyClave() : null,
                p.getSiguiente() == null ? null : p.getSiguiente().getId()
        );
    }
}
