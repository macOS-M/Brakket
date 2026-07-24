package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.MiembroEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MiembroEquipoRepository extends JpaRepository<MiembroEquipo, Long> {

    List<MiembroEquipo> findByEquipoId(Long equipoId);

    /** ¿El usuario ya milita ACTIVO en algún equipo? (un roster a la vez) */
    boolean existsByUsuarioIdAndEstado(Long usuarioId, String estado);

    /**
     * ¿La plantilla registró bajas/expulsiones? Cuenta como historial del
     * equipo (RF-16): el borrado físico no debe destruir esa trazabilidad.
     */
    boolean existsByEquipoIdAndFechaBajaIsNotNull(Long equipoId);

    List<MiembroEquipo> findByEquipoIdAndEstado(Long equipoId, String estado);

    @Query("""
            select m.equipo.id, count(m) from MiembroEquipo m
            where m.equipo.id in :equipoIds and m.estado = 'ACTIVO'
            group by m.equipo.id""")
    List<Object[]> contarActivosPorEquipo(@Param("equipoIds") List<Long> equipoIds);

    List<MiembroEquipo> findByUsuarioId(Long usuarioId);

    @Query("""
            select m from MiembroEquipo m
            join fetch m.equipo
            where m.usuario.id in :usuarioIds and m.estado = 'ACTIVO'""")
    List<MiembroEquipo> findActivosByUsuarioIds(@Param("usuarioIds") List<Long> usuarioIds);

    Optional<MiembroEquipo> findByEquipoIdAndUsuarioId(Long equipoId, Long usuarioId);

    long countByEquipoIdAndRolAndEstado(Long equipoId, String rol, String estado);

    @Query("""
            select m from MiembroEquipo m
            join fetch m.equipo e
            left join fetch e.juego
            where m.usuario.id = :usuarioId
              and (:juegoId is null or e.juego.id = :juegoId)
              and (:desde is null or m.fechaUnion >= :desde)
              and (:hasta is null or m.fechaUnion <= :hasta)
            order by m.fechaUnion desc
            """)
    List<MiembroEquipo> historialDeJugador(@Param("usuarioId") Long usuarioId,
                                           @Param("juegoId") Long juegoId,
                                           @Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta);
}