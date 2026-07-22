package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    Collection<EstadoTorneo> ESTADOS_CERRADOS =
            EnumSet.of(EstadoTorneo.FINALIZADO, EstadoTorneo.CANCELADO);

    List<Torneo> findByTemporadaId(Long temporadaId);

    boolean existsByTemporadaLigaJuegoIdAndEstadoNotIn(
            Long juegoId, Collection<EstadoTorneo> estadosCerrados);

    boolean existsByTemporadaIdAndEstadoNotIn(
            Long temporadaId, Collection<EstadoTorneo> estadosCerrados);

    default boolean existsActivoByJuegoId(Long juegoId) {
        return existsByTemporadaLigaJuegoIdAndEstadoNotIn(juegoId, ESTADOS_CERRADOS);
    }

    default boolean existsActivoByTemporadaId(Long temporadaId) {
        return existsByTemporadaIdAndEstadoNotIn(temporadaId, ESTADOS_CERRADOS);
    }
}
