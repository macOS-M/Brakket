package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * La búsqueda de equipos (RF-05) usa {@link JpaSpecificationExecutor}: los
 * filtros son opcionales y la consulta se arma solo con los presentes (ver
 * {@code TeamSearchServiceImpl}).
 */
public interface EquipoRepository extends JpaRepository<Equipo, Long>,
        JpaSpecificationExecutor<Equipo> {

    Optional<Equipo> findByNombre(String nombre);
}
