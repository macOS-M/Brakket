package com.coffeecommits.brakket.league.repository;

import com.coffeecommits.brakket.league.model.Liga;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigaRepository extends JpaRepository<Liga, Long> {

    boolean existsByJuegoId(Long juegoId);
}