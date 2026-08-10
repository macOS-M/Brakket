package com.coffeecommits.brakket.game.repository;

import com.coffeecommits.brakket.game.model.Juego;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JuegoRepository extends JpaRepository<Juego, Long> {

    Optional<Juego> findByNombre(String nombre);

    Optional<Juego> findByNombreIgnoreCase(String nombre);

    List<Juego> findByActivoTrue();

    /** Juegos activos del catálogo: métrica del panel global (RF-49). */
    long countByActivoTrue();

    /** Valida el filtro de disciplina de la búsqueda de equipos (RF-05). */
    boolean existsByGeneroIgnoreCase(String genero);
}