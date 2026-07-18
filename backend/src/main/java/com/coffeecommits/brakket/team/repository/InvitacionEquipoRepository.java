package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.InvitacionEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitacionEquipoRepository extends JpaRepository<InvitacionEquipo, Long> {

    boolean existsByEquipoIdAndJugadorIdAndEstado(Long equipoId, Long jugadorId, String estado);

    List<InvitacionEquipo> findByJugadorIdAndEstado(Long jugadorId, String estado);

    Optional<InvitacionEquipo> findByIdAndJugadorId(Long id, Long jugadorId);
}