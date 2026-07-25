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

    /**
     * Partidas que alimentan a esta (su ganador o su perdedor ocupa uno de
     * sus slots): permiten saber si un slot vacío ya no tiene quién lo llene
     * y la partida debe cerrarse como bye.
     */
    @Query("select p from Partida p where p.siguiente.id = :id or p.perdedorSiguiente.id = :id")
    List<Partida> alimentadoresDe(@Param("id") Long id);

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

    /**
     * Victorias reales del equipo: partidas FINALIZADAS con ambos rivales
     * (un bye no es una partida jugada) donde el ganador es el equipo.
     * Acceso solo por id de las asociaciones: usa la columna FK sin join.
     */
    @Query("""
            select count(p) from Partida p
            where p.estado = com.coffeecommits.brakket.tournament.model.EstadoPartida.FINALIZADA
              and p.equipoA.id is not null and p.equipoB.id is not null
              and p.ganador.id = :equipoId
            """)
    long victoriasDe(@Param("equipoId") Long equipoId);

    /** Derrotas reales: jugó (ambos rivales), hubo ganador y no fue él. */
    @Query("""
            select count(p) from Partida p
            where p.estado = com.coffeecommits.brakket.tournament.model.EstadoPartida.FINALIZADA
              and p.equipoA.id is not null and p.equipoB.id is not null
              and (p.equipoA.id = :equipoId or p.equipoB.id = :equipoId)
              and p.ganador.id is not null and p.ganador.id <> :equipoId
            """)
    long derrotasDe(@Param("equipoId") Long equipoId);

    // Historial competitivo del equipo (bloquea el borrado físico). Van como
    // tres derivadas separadas: un OR de paths implícitos en JPQL generaría
    // inner joins que ignoran las filas con el otro equipo en null.
    boolean existsByEquipoAId(Long equipoId);

    boolean existsByEquipoBId(Long equipoId);

    boolean existsByGanadorId(Long equipoId);
}
