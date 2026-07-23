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

    /** Torneos públicos de un juego, próximos primero. */
    List<Torneo> findByJuegoIdAndPublicoTrueOrderByFechaInicioAsc(Long juegoId);

    /** Todos los torneos públicos (listado global), próximos primero. */
    List<Torneo> findByPublicoTrueOrderByFechaInicioAsc();

    /** Torneos organizados por un usuario (incluye privados), próximos primero. */
    List<Torneo> findByOrganizadorIdOrderByFechaInicioAsc(Long organizadorId);

    /** Desde el modelo abierto (V22) todo torneo referencia su juego directo. */
    boolean existsByJuegoIdAndEstadoNotIn(Long juegoId, Collection<EstadoTorneo> estadosCerrados);

    boolean existsByTemporadaIdAndEstadoNotIn(
            Long temporadaId, Collection<EstadoTorneo> estadosCerrados);

    default boolean existsActivoByJuegoId(Long juegoId) {
        return existsByJuegoIdAndEstadoNotIn(juegoId, ESTADOS_CERRADOS);
    }

    default boolean existsActivoByTemporadaId(Long temporadaId) {
        return existsByTemporadaIdAndEstadoNotIn(temporadaId, ESTADOS_CERRADOS);
    }
}
