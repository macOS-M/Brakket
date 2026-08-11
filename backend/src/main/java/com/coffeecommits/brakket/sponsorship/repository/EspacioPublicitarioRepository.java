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

    // RF-44 review: cuenta espacios sin traer las entidades completas a Java,
    // evita cargar filas enteras solo para contar (usado en el resumen del panel).
    long countByPatrocinioId(Long patrocinioId);

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