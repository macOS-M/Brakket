package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    List<Partida> findByTorneoId(Long torneoId);

    /** El bracket completo en orden de dibujo: ronda a ronda, slot a slot. */
    List<Partida> findByTorneoIdOrderByRondaAscOrdenAsc(Long torneoId);

    boolean existsByTorneoId(Long torneoId);

    /**
     * RF-03: partidas pendientes que impiden disolver un equipo. Una
     * partida FINALIZADA o CANCELADA ya no ancla al equipo.
     */
    @Query("""
            select count(p) > 0 from Partida p
            where (p.equipoA.id = :equipoId or p.equipoB.id = :equipoId)
              and p.estado not in (
                  com.coffeecommits.brakket.tournament.model.EstadoPartida.FINALIZADA,
                  com.coffeecommits.brakket.tournament.model.EstadoPartida.CANCELADA)
            """)
    boolean existsPartidaPendientePorEquipo(@Param("equipoId") Long equipoId);
}
