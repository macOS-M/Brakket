package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    List<Torneo> findByTemporadaId(Long temporadaId);

    /** Torneos públicos de un juego, próximos primero. */
    List<Torneo> findByJuegoIdAndPublicoTrueOrderByFechaInicioAsc(Long juegoId);

    /** Todos los torneos públicos (listado global), próximos primero. */
    List<Torneo> findByPublicoTrueOrderByFechaInicioAsc();

    /** Torneos organizados por un usuario (incluye privados), próximos primero. */
    List<Torneo> findByOrganizadorIdOrderByFechaInicioAsc(Long organizadorId);

    @Query("""
            select count(t) > 0 from Torneo t
            where t.juego.id = :juegoId
              and upper(t.estado) not in ('FINALIZADO', 'CANCELADO')
            """)
    boolean existsActivoByJuegoId(@Param("juegoId") Long juegoId);
}
