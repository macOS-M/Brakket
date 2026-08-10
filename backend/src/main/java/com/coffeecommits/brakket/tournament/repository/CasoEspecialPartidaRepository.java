package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.CasoEspecialPartida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CasoEspecialPartidaRepository extends JpaRepository<CasoEspecialPartida, Long> {

    List<CasoEspecialPartida> findByPartidaIdOrderByFechaDesc(Long partidaId);
}