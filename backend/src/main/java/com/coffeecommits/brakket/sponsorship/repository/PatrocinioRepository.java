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

    // Rediseño: el recurso escaso ahora es la competencia entera (liga o
    // torneo), no (competencia, nivel) — un solo patrocinio ACTIVO a la vez
    // por liga y uno por torneo, sin importar quién sea. TEMPORADA ya no
    // participa (alcance retirado del flujo de creación).
    @Query("""
            SELECT COUNT(p) > 0 FROM Patrocinio p
            WHERE p.estado = 'ACTIVO'
            AND (
                (:ligaId IS NOT NULL AND p.liga.id = :ligaId)
                OR (:torneoId IS NOT NULL AND p.torneo.id = :torneoId)
            )
            AND p.fechaInicio <= :fechaFin
            AND p.fechaFin >= :fechaInicio
            """)
    boolean existeSolapamiento(@Param("ligaId") Long ligaId,
                               @Param("torneoId") Long torneoId,
                               @Param("fechaInicio") LocalDate fechaInicio,
                               @Param("fechaFin") LocalDate fechaFin);

    // RF-50: filtro combinado para el reporte de patrocinios. Los parámetros nulos
    // se ignoran (mismo patrón IS NULL OR que existeSolapamiento). El rango de fechas
    // es de solapamiento (igual que la validación de creación), no coincidencia exacta.
    @Query("""
            SELECT p FROM Patrocinio p
            WHERE (:torneoId IS NULL OR p.torneo.id = :torneoId)
              AND (:patrocinadorId IS NULL OR p.patrocinador.id = :patrocinadorId)
              AND (:desde IS NULL OR p.fechaFin >= :desde)
              AND (:hasta IS NULL OR p.fechaInicio <= :hasta)
            ORDER BY p.fechaInicio DESC
            """)
    List<Patrocinio> buscarParaReporte(@Param("torneoId") Long torneoId,
                                       @Param("patrocinadorId") Long patrocinadorId,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);
}