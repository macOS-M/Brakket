package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.EquipoRolHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipoRolHistorialRepository extends JpaRepository<EquipoRolHistorial, Long> {

    List<EquipoRolHistorial> findByEquipoIdOrderByFechaDesc(Long equipoId);
}