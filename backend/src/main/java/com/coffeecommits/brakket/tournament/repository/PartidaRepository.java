package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    List<Partida> findByTorneoId(Long torneoId);
}
