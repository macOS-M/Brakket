package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    Optional<Equipo> findByNombre(String nombre);
}
