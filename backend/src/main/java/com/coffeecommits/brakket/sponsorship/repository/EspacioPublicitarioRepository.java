package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.EspacioPublicitario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EspacioPublicitarioRepository extends JpaRepository<EspacioPublicitario, Long> {

    List<EspacioPublicitario> findByPatrocinioId(Long patrocinioId);

    // "Ocupado" = misma competencia (via patrocinio) + misma ubicacion + activo
    // + fechas del patrocinio dueno se cruzan con el rango dado.
    // excluyendoEspacioId permite reusar esta query en ediciones (no implementadas
    // aun, pero se deja preparada) sin que un espacio choque consigo mismo.
    @Query("""
            SELECT COUNT(e) > 0 FROM EspacioPublicitario e
            JOIN e.patrocinio p
            WHERE e.estado = 'ACTIVO'
            AND e.ubicacion = :ubicacion
            AND (
                (:ligaId IS NOT NULL AND p.liga.id = :ligaId)
                OR (:temporadaId IS NOT NULL AND p.temporada.id = :temporadaId)
                OR (:torneoId IS NOT NULL AND p.torneo.id = :torneoId)
            )
            AND p.fechaInicio <= :fechaFin
            AND p.fechaFin >= :fechaInicio
            AND (:excluyendoEspacioId IS NULL OR e.id <> :excluyendoEspacioId)
            """)
    boolean existeEspacioOcupado(@Param("ligaId") Long ligaId,
                                 @Param("temporadaId") Long temporadaId,
                                 @Param("torneoId") Long torneoId,
                                 @Param("ubicacion") String ubicacion,
                                 @Param("fechaInicio") LocalDate fechaInicio,
                                 @Param("fechaFin") LocalDate fechaFin,
                                 @Param("excluyendoEspacioId") Long excluyendoEspacioId);

    // Espacio vigente para una ubicacion + alcance especifico, usado por el
    // componente publico <app-ad-slot>. "Vigente" = ACTIVO y la fecha de hoy
    // cae dentro del rango heredado del patrocinio dueno.
    @Query("""
            SELECT e FROM EspacioPublicitario e
            JOIN e.patrocinio p
            WHERE e.estado = 'ACTIVO'
            AND e.ubicacion = :ubicacion
            AND (
                (:ligaId IS NOT NULL AND p.liga.id = :ligaId)
                OR (:temporadaId IS NOT NULL AND p.temporada.id = :temporadaId)
                OR (:torneoId IS NOT NULL AND p.torneo.id = :torneoId)
            )
            AND p.fechaInicio <= :hoy
            AND p.fechaFin >= :hoy
            """)
    Optional<EspacioPublicitario> buscarVigente(@Param("ligaId") Long ligaId,
                                                @Param("temporadaId") Long temporadaId,
                                                @Param("torneoId") Long torneoId,
                                                @Param("ubicacion") String ubicacion,
                                                @Param("hoy") LocalDate hoy);
}