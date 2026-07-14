package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.MiembroEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MiembroEquipoRepository extends JpaRepository<MiembroEquipo, Long> {

    List<MiembroEquipo> findByEquipoId(Long equipoId);

    List<MiembroEquipo> findByEquipoIdAndEstado(Long equipoId, String estado);

    /**
     * Cantidad de miembros activos por equipo, en una sola query para todo el
     * listado público (evita un count por equipo).
     */
    @Query("""
            select m.equipo.id, count(m) from MiembroEquipo m
            where m.equipo.id in :equipoIds and m.estado = 'ACTIVO'
            group by m.equipo.id""")
    List<Object[]> contarActivosPorEquipo(@Param("equipoIds") List<Long> equipoIds);

    List<MiembroEquipo> findByUsuarioId(Long usuarioId);

    Optional<MiembroEquipo> findByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);

    long countByEquipoIdAndRolAndEstado(Long equipoId, String rol, String estado);
}
