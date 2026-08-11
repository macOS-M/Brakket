package com.coffeecommits.brakket.analytics.repository;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalisisSentimientoRepository extends JpaRepository<AnalisisSentimiento, Long> {

    List<AnalisisSentimiento> findByMetricaChatId(Long metricaChatId);

    // RF-44 review: reemplaza el N+1 de calcularSentimientoPredominante
    // (una consulta por cada MetricaChat de la transmision). Agrupa por
    // clasificacion en una sola query, anclado en metricaChat.transmisionTwitch.
    @Query("""
            SELECT a.clasificacion as clasificacion, COUNT(a) as cantidad
            FROM AnalisisSentimiento a
            WHERE a.metricaChat.transmisionTwitch.id = :transmisionId
            GROUP BY a.clasificacion
            """)
    List<ConteoClasificacion> contarPorClasificacionDeTransmision(@Param("transmisionId") Long transmisionId);

    interface ConteoClasificacion {
        String getClasificacion();
        Long getCantidad();
    }

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