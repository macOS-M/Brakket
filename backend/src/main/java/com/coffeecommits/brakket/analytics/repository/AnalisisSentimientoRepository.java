package com.coffeecommits.brakket.analytics.repository;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalisisSentimientoRepository extends JpaRepository<AnalisisSentimiento, Long> {

    List<AnalisisSentimiento> findByMetricaChatId(Long metricaChatId);

    /**
     * RF-37: sentimiento de una transmisión dentro de un rango. La entidad no la
     * referencia directamente, así que se ancla vía metricaChat. El join es INNER:
     * descarta el análisis colgado de métricas de chat sin transmisión asociada.
     */
    @Query("""
            select a from AnalisisSentimiento a
            where a.metricaChat.transmisionTwitch.id = :transmisionId
              and a.fechaHora >= :desde and a.fechaHora <= :hasta
            order by a.fechaHora asc""")
    List<AnalisisSentimiento> buscarPorTransmisionYRango(@Param("transmisionId") Long transmisionId,
                                                         @Param("desde") LocalDateTime desde,
                                                         @Param("hasta") LocalDateTime hasta);

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
