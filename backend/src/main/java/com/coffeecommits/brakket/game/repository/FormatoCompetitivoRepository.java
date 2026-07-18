package com.coffeecommits.brakket.game.repository;

import com.coffeecommits.brakket.game.model.FormatoCompetitivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormatoCompetitivoRepository
        extends JpaRepository<FormatoCompetitivo, Long> {
    List<FormatoCompetitivo> findByActivoTrueOrderByNombreAsc();
}
