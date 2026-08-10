package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Regla de "quien esta relacionado con una partida en disputa" (capitan
 * de cualquiera de los 2 equipos, organizador, arbitro del torneo o
 * admin) y de "que estados de disputa siguen abiertos", compartida entre
 * DisputaServiceImpl, EvidenciaServiceImpl y ApelacionServiceImpl para
 * no repetir la misma logica en cada uno.
 */
@Component
public class DisputaGuard {

    public static final List<String> ESTADOS_ACTIVOS = List.of("PENDIENTE", "EN_REVISION");

    private final InscripcionRepository inscripcionRepository;
    private final ArbitroTorneoRepository arbitroTorneoRepository;

    public DisputaGuard(InscripcionRepository inscripcionRepository,
                        ArbitroTorneoRepository arbitroTorneoRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.arbitroTorneoRepository = arbitroTorneoRepository;
    }

    public boolean estaActiva(String estadoDisputa) {
        return ESTADOS_ACTIVOS.contains(estadoDisputa);
    }

    public void exigirRelacionado(Partida partida, Usuario usuario, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        Torneo torneo = partida.getTorneo();
        boolean esOrganizador = torneo.getOrganizador().getId().equals(usuario.getId());
        boolean esArbitro = arbitroTorneoRepository.findByTorneoId(torneo.getId()).stream()
                .anyMatch(a -> a.getUsuario().getId().equals(usuario.getId()));
        boolean esCapitanA = partida.getEquipoA() != null
                && inscripcionRepository.esCapitanActivo(usuario.getId(), partida.getEquipoA().getId());
        boolean esCapitanB = partida.getEquipoB() != null
                && inscripcionRepository.esCapitanActivo(usuario.getId(), partida.getEquipoB().getId());

        if (!esOrganizador && !esArbitro && !esCapitanA && !esCapitanB) {
            throw new ForbiddenException(
                    "Solo un capitan de la partida, el organizador o un arbitro del torneo "
                            + "pueden actuar sobre esta disputa");
        }
    }
}