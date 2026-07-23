package com.coffeecommits.brakket.tournament.dto;

import com.coffeecommits.brakket.tournament.model.Partida;

/**
 * Partida del bracket aplanada para el frontend. La lobby es visible para
 * cualquiera que vea el torneo: en la demo simplifica; restringirla a los
 * participantes queda como deuda registrada.
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
    public static PartidaResponse from(Partida p) {
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
                p.getLobbyClave(),
                p.getSiguiente() == null ? null : p.getSiguiente().getId()
        );
    }
}
