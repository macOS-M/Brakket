package com.coffeecommits.brakket.analytics.repository;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalisisSentimientoRepository extends JpaRepository<AnalisisSentimiento, Long> {

    List<AnalisisSentimiento> findByMetricaChatId(Long metricaChatId);

    /**
     * Serie de análisis de una transmisión, del más antiguo al más reciente
     * (RF-40). Navega analisis_sentimiento → metrica_chat → transmision_twitch.
     */
    @Query("""
            select a from AnalisisSentimiento a
            join a.metricaChat m
            where m.transmisionTwitch.id = :transmisionId
            order by a.fechaHora asc, a.id asc""")
    List<AnalisisSentimiento> findSerieByTransmision(@Param("transmisionId") Long transmisionId);
}
