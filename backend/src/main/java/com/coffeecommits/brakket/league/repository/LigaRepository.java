package com.coffeecommits.brakket.league.repository;

import com.coffeecommits.brakket.league.model.Liga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigaRepository extends JpaRepository<Liga, Long> {

    /** Ligas ordenadas de la más reciente a la más antigua (para el listado). */
    List<Liga> findAllByOrderByIdDesc();

    /** Evita que un mismo comisionado repita el nombre de liga (regla de negocio RF-22). */
    boolean existsByComisionadoIdAndNombreIgnoreCase(Long comisionadoId, String nombre);

    boolean existsByJuegoId(Long juegoId);
}
