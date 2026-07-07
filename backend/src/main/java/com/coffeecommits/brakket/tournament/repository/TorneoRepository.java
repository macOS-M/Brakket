package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    List<Torneo> findByTemporadaId(Long temporadaId);
}
