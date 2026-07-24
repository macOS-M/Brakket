package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.SolicitudUnion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudUnionRepository extends JpaRepository<SolicitudUnion, Long> {

    boolean existsByEquipoIdAndJugadorIdAndEstado(Long equipoId, Long jugadorId, String estado);

    List<SolicitudUnion> findByEquipoIdAndEstadoOrderByFechaCreacionAsc(Long equipoId, String estado);

    List<SolicitudUnion> findByJugadorIdAndEstado(Long jugadorId, String estado);

    Optional<SolicitudUnion> findById(Long id);
}
