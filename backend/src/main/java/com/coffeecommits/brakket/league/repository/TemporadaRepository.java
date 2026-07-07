package com.coffeecommits.brakket.league.repository;

import com.coffeecommits.brakket.league.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findByLigaId(Long ligaId);
}
