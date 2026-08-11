package com.coffeecommits.brakket.league.repository;

import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigaRepository extends JpaRepository<Liga, Long> {

    /** Ligas ordenadas de la más reciente a la más antigua (para el listado). */
    List<Liga> findAllByOrderByIdDesc();

    /** Evita que un mismo comisionado repita el nombre de liga (regla de negocio RF-22). */
    boolean existsByComisionadoIdAndNombreIgnoreCase(Long comisionadoId, String nombre);

    boolean existsByJuegoId(Long juegoId);

    @Query("""
            select new com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse(l.id, l.nombre)
            from Liga l
            where exists (
                select p.id from Partida p
                where p.torneo.temporada.liga = l
                  and p.estado = com.coffeecommits.brakket.tournament.model.EstadoPartida.FINALIZADA
                  and p.equipoA is not null and p.equipoB is not null
            )
            order by l.nombre
            """)
    List<OpcionEstadisticaResponse> buscarOpcionesConResultadosOficiales();
}
