package com.coffeecommits.brakket.progression.repository;

import com.coffeecommits.brakket.progression.model.LogroJugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogroJugadorRepository extends JpaRepository<LogroJugador, Long> {

    List<LogroJugador> findByUsuarioId(Long usuarioId);
}
