package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.tournament.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByTorneoId(Long torneoId);

    List<Inscripcion> findByEquipoId(Long equipoId);

    boolean existsByTorneoIdAndEquipoId(Long torneoId, Long equipoId);

    /** ¿El equipo tiene CUALQUIER inscripción (histórica o vigente)? */
    boolean existsByEquipoId(Long equipoId);

    /** Inscripciones vigentes de un torneo (las que ocupan cupo). */
    @Query("""
            select count(i) from Inscripcion i
            where i.torneo.id = :torneoId
              and i.estado not in ('RECHAZADA', 'CANCELADA')
            """)
    long countVigentesPorTorneo(@Param("torneoId") Long torneoId);

    /**
     * RF-03: inscripciones que impiden disolver un equipo. Se consideran
     * cerradas las RECHAZADA/CANCELADA/FINALIZADA y, con el ciclo de vida
     * del torneo, también cualquier inscripción de un torneo ya cerrado
     * (FINALIZADO/CANCELADO): un campeonato terminado no ancla al equipo.
     */
    @Query("""
            select count(i) > 0 from Inscripcion i
            where i.equipo.id = :equipoId
              and i.estado not in ('RECHAZADA', 'CANCELADA', 'FINALIZADA')
              and i.torneo.estado not in (
                  com.coffeecommits.brakket.tournament.model.EstadoTorneo.FINALIZADO,
                  com.coffeecommits.brakket.tournament.model.EstadoTorneo.CANCELADO)
            """)
    boolean existsInscripcionActivaPorEquipo(@Param("equipoId") Long equipoId);

    /**
     * Torneos donde compite el usuario: inscripciones vigentes de equipos
     * en los que es miembro activo (para el "Tus competencias" del panel).
     */
    @Query("""
            select i from Inscripcion i
            where i.estado not in ('RECHAZADA', 'CANCELADA')
              and exists (
                  select 1 from MiembroEquipo m
                  where m.equipo.id = i.equipo.id
                    and m.usuario.id = :usuarioId
                    and m.estado = 'ACTIVO')
            """)
    List<Inscripcion> inscripcionesVigentesDeUsuario(@Param("usuarioId") Long usuarioId);

    /** Inscripciones vigentes en orden de llegada: la siembra del bracket. */
    @Query("""
            select i from Inscripcion i
            join fetch i.equipo
            where i.torneo.id = :torneoId
              and i.estado not in ('RECHAZADA', 'CANCELADA')
            order by i.id
            """)
    List<Inscripcion> vigentesPorTorneo(@Param("torneoId") Long torneoId);

    /**
     * Equipos activos donde el usuario es capitán activo (RF-25: solo el
     * capitán inscribe). Vive acá y no en el repo de equipos para no tocar
     * archivos en revisión de otros PRs.
     */
    @Query("""
            select m.equipo from MiembroEquipo m
            where m.usuario.id = :usuarioId
              and m.rol = 'CAPITAN'
              and m.estado = 'ACTIVO'
              and m.equipo.estado = 'ACTIVO'
            """)
    List<Equipo> equiposCapitaneadosPor(@Param("usuarioId") Long usuarioId);

    @Query("""
            select count(m) > 0 from MiembroEquipo m
            where m.usuario.id = :usuarioId
              and m.equipo.id = :equipoId
              and m.rol = 'CAPITAN'
              and m.estado = 'ACTIVO'
            """)
    boolean esCapitanActivo(@Param("usuarioId") Long usuarioId, @Param("equipoId") Long equipoId);

    /**
     * Equipos inscritos en el torneo cuyo capitán activo es el usuario: los
     * únicos cruces cuya clave de lobby puede ver (además del organizador).
     */
    @Query("""
            select i.equipo.id from Inscripcion i
            where i.torneo.id = :torneoId
              and i.estado not in ('RECHAZADA', 'CANCELADA')
              and exists (
                  select 1 from MiembroEquipo m
                  where m.equipo.id = i.equipo.id
                    and m.usuario.id = :usuarioId
                    and m.rol = 'CAPITAN'
                    and m.estado = 'ACTIVO')
            """)
    List<Long> equiposCapitaneadosEnTorneo(@Param("usuarioId") Long usuarioId,
                                           @Param("torneoId") Long torneoId);

    @Query("""
            select count(m) from MiembroEquipo m
            where m.equipo.id = :equipoId and m.estado = 'ACTIVO'
            """)
    long countMiembrosActivos(@Param("equipoId") Long equipoId);

    /** Plantilla activa de un equipo, para el detalle del torneo (RF-25). */
    @Query("""
            select m from MiembroEquipo m
            join fetch m.usuario
            where m.equipo.id = :equipoId and m.estado = 'ACTIVO'
            order by case m.rol when 'CAPITAN' then 0 else 1 end, m.id
            """)
    List<MiembroEquipo> miembrosActivosDeEquipo(@Param("equipoId") Long equipoId);
}
