package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.Partida;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    List<Partida> findByTorneoId(Long torneoId);

    /**
     * Lectura con bloqueo de fila: reportar/confirmar/rechazar/resolver y el
     * avance de la llave serializan sobre la partida para que dos capitanes
     * (o dos semifinales escribiendo la misma final) no se pisen el estado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Partida p where p.id = :id")
    Optional<Partida> bloquearPorId(@Param("id") Long id);

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
