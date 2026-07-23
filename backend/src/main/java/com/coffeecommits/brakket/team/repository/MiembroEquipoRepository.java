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

    /**
     * Membresías activas de un lote de usuarios, con el equipo ya traído, para
     * armar los badges de "requiere transferencia" en una sola query en vez de
     * una por candidato.
     */
    @Query("""
            select m from MiembroEquipo m
            join fetch m.equipo
            where m.usuario.id in :usuarioIds and m.estado = 'ACTIVO'""")
    List<MiembroEquipo> findActivosByUsuarioIds(@Param("usuarioIds") List<Long> usuarioIds);

    Optional<MiembroEquipo> findByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);

    long countByEquipoIdAndRolAndEstado(Long equipoId, String rol, String estado);
}
