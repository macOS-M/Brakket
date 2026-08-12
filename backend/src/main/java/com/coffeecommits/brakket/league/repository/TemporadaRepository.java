package com.coffeecommits.brakket.league.repository;

import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findByLigaIdOrderByFechaInicioAsc(Long ligaId);

    boolean existsByLigaIdAndNombreIgnoreCase(Long ligaId, String nombre);

    boolean existsByLigaIdAndNombreIgnoreCaseAndIdNot(Long ligaId, String nombre, Long id);

    /**
     * Existe una temporada de la liga cuyo rango [fechaInicio, fechaFin] se
     * solapa con el rango recibido (inicio <= finNueva && fin >= inicioNueva).
     */
    boolean existsByLigaIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Long ligaId, LocalDate fechaFinNueva, LocalDate fechaInicioNueva);

    boolean existsByLigaIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualAndIdNot(
            Long ligaId, LocalDate fechaFinNueva, LocalDate fechaInicioNueva, Long id);

    void deleteByLigaId(Long ligaId);

    @Query("""
            select new com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse(s.id, s.nombre)
            from Temporada s
            where exists (
                select p.id from Partida p
                where p.torneo.temporada = s
                  and p.estado = com.coffeecommits.brakket.tournament.model.EstadoPartida.FINALIZADA
                  and p.equipoA is not null and p.equipoB is not null
            )
            order by s.nombre
            """)
    List<OpcionEstadisticaResponse> buscarOpcionesConResultadosOficiales();
}
