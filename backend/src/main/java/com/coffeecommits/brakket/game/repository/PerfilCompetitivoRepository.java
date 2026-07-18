package com.coffeecommits.brakket.game.repository;

import com.coffeecommits.brakket.game.model.PerfilCompetitivoJuego;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilCompetitivoRepository
        extends JpaRepository<PerfilCompetitivoJuego, Long> {

    Optional<PerfilCompetitivoJuego> findByJuegoId(Long juegoId);

}