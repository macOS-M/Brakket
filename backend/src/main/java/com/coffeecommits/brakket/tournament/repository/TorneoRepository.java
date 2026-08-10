package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Torneo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    Collection<EstadoTorneo> ESTADOS_CERRADOS =
            EnumSet.of(EstadoTorneo.FINALIZADO, EstadoTorneo.CANCELADO);

    List<Torneo> findByTemporadaId(Long temporadaId);

    /**
     * Lectura con bloqueo de fila: el cierre de etapa (última jornada de
     * liga/suizo, cierre de grupos) se serializa sobre el torneo para que
     * dos partidas confirmándose a la vez no se "vean" mutuamente abiertas
     * y la etapa quede sin cerrar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Torneo t where t.id = :id")
    Optional<Torneo> bloquearPorId(@Param("id") Long id);

    /** ¿El equipo figura como campeón de algún torneo? (historial protegido) */
    boolean existsByCampeonId(Long equipoId);

    /** Torneos públicos de un juego, próximos primero. */
    List<Torneo> findByJuegoIdAndPublicoTrueOrderByFechaInicioAsc(Long juegoId);

    List<Torneo> findByPublicoTrueOrderByFechaInicioAsc();

    List<Torneo> findByOrganizadorIdOrderByFechaInicioAsc(Long organizadorId);

    /** Conteo de torneos por estado: métrica del panel global (RF-49). */
    long countByEstado(EstadoTorneo estado);

    boolean existsByJuegoIdAndEstadoNotIn(Long juegoId, Collection<EstadoTorneo> estadosCerrados);

    boolean existsByTemporadaIdAndEstadoNotIn(
            Long temporadaId, Collection<EstadoTorneo> estadosCerrados);

    default boolean existsActivoByJuegoId(Long juegoId) {
        return existsByJuegoIdAndEstadoNotIn(juegoId, ESTADOS_CERRADOS);
    }

    default boolean existsActivoByTemporadaId(Long temporadaId) {
        return existsByTemporadaIdAndEstadoNotIn(temporadaId, ESTADOS_CERRADOS);
    }

    @Query("""
            select distinct t from Torneo t
            where t.publico = true or t.organizador.id = :usuarioId or :esAdmin = true
            order by t.fechaInicio asc
            """)
    List<Torneo> buscarVisiblesParaCalendario(@Param("usuarioId") Long usuarioId,
                                              @Param("esAdmin") boolean esAdmin);
}