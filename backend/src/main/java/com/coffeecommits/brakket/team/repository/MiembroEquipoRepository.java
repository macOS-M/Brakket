package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.MiembroEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MiembroEquipoRepository extends JpaRepository<MiembroEquipo, Long> {

    List<MiembroEquipo> findByEquipoId(Long equipoId);

    List<MiembroEquipo> findByUsuarioId(Long usuarioId);
}
