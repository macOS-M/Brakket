package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.ArbitroTorneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArbitroTorneoRepository extends JpaRepository<ArbitroTorneo, Long> {

    List<ArbitroTorneo> findByTorneoId(Long torneoId);
}
