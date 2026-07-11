package com.coffeecommits.brakket.tournament.repository;

import com.coffeecommits.brakket.tournament.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByTorneoId(Long torneoId);

    /**
     * RF-03: inscripciones que impiden disolver un equipo. Se consideran
     * cerradas las RECHAZADA/CANCELADA/FINALIZADA (convención pendiente de
     * formalizar cuando exista la gestión de inscripciones, RF-25).
     */
    @Query("""
            select count(i) > 0 from Inscripcion i
            where i.equipo.id = :equipoId
              and i.estado not in ('RECHAZADA', 'CANCELADA', 'FINALIZADA')
            """)
    boolean existsInscripcionActivaPorEquipo(@Param("equipoId") Long equipoId);
}
