package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.MiembroEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MiembroEquipoRepository extends JpaRepository<MiembroEquipo, Long> {

    List<MiembroEquipo> findByEquipoId(Long equipoId);

    List<MiembroEquipo> findByEquipoIdAndEstado(Long equipoId, String estado);

    List<MiembroEquipo> findByUsuarioId(Long usuarioId);

    Optional<MiembroEquipo> findByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);

    long countByEquipoIdAndRolAndEstado(Long equipoId, String rol, String estado);
}
