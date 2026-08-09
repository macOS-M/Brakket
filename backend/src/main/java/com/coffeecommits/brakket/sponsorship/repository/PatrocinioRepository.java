package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PatrocinioRepository extends JpaRepository<Patrocinio, Long> {

    List<Patrocinio> findByTorneoId(Long torneoId);

    List<Patrocinio> findByLigaId(Long ligaId);

    List<Patrocinio> findByTemporadaId(Long temporadaId);

    // RF-44: patrocinios de una marca, para el panel comercial del patrocinador.
    List<Patrocinio> findByPatrocinadorId(Long patrocinadorId);

    // Solo uno de ligaId/temporadaId/torneoId llega no-nulo (validado antes de llamar
    // este método), por eso el OR funciona: solo compara contra el alcance real enviado.
    @Query("""
            SELECT COUNT(p) > 0 FROM Patrocinio p
            WHERE p.estado = 'ACTIVO'
            AND (
                (:ligaId IS NOT NULL AND p.liga.id = :ligaId)
                OR (:temporadaId IS NOT NULL AND p.temporada.id = :temporadaId)
                OR (:torneoId IS NOT NULL AND p.torneo.id = :torneoId)
            )
            AND p.fechaInicio <= :fechaFin
            AND p.fechaFin >= :fechaInicio
            """)
    boolean existeSolapamiento(@Param("ligaId") Long ligaId,
                               @Param("temporadaId") Long temporadaId,
                               @Param("torneoId") Long torneoId,
                               @Param("fechaInicio") LocalDate fechaInicio,
                               @Param("fechaFin") LocalDate fechaFin);
}