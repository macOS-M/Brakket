package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    Collection<EstadoTorneo> ESTADOS_CERRADOS =
            EnumSet.of(EstadoTorneo.FINALIZADO, EstadoTorneo.CANCELADO);

    List<Torneo> findByTemporadaId(Long temporadaId);

    List<Torneo> findByJuegoIdAndPublicoTrueOrderByFechaInicioAsc(Long juegoId);

    List<Torneo> findByPublicoTrueOrderByFechaInicioAsc();

    List<Torneo> findByOrganizadorIdOrderByFechaInicioAsc(Long organizadorId);

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