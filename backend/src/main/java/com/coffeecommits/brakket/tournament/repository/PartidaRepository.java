package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    List<Partida> findByTorneoId(Long torneoId);

    /**
     * RF-03: partidas pendientes que impiden disolver un equipo. Se considera
     * resuelta una partida FINALIZADA o CANCELADA (convención pendiente de
     * formalizar cuando exista el motor de competencias, RF-26+).
     */
    @Query("""
            select count(p) > 0 from Partida p
            where (p.equipoA.id = :equipoId or p.equipoB.id = :equipoId)
              and p.estado not in ('FINALIZADA', 'CANCELADA')
            """)
    boolean existsPartidaPendientePorEquipo(@Param("equipoId") Long equipoId);
}
