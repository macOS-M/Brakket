package com.coffeecommits.brakket.game.repository;

import com.coffeecommits.brakket.game.model.EstadisticaJuego;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstadisticaJuegoRepository
        extends JpaRepository<EstadisticaJuego, Long> {

    List<EstadisticaJuego> findByObligatoriaTrueAndActivaTrue();

    List<EstadisticaJuego> findByActivaTrueOrderByNombreAsc();

}
