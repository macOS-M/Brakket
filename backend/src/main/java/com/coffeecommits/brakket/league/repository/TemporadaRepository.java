package com.coffeecommits.brakket.league.repository;

import com.coffeecommits.brakket.league.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findByLigaId(Long ligaId);

    /**
     * Existe una temporada de la liga cuyo rango [fechaInicio, fechaFin] se
     * solapa con el rango recibido (inicio <= finNueva && fin >= inicioNueva).
     */
    boolean existsByLigaIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Long ligaId, LocalDate fechaFinNueva, LocalDate fechaInicioNueva);

    void deleteByLigaId(Long ligaId);
}
