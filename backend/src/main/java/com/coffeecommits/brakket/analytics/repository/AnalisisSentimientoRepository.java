package com.coffeecommits.brakket.analytics.repository;

import com.coffeecommits.brakket.analytics.dto.TransmisionAnalizadaResponse;
import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * Igual que {@link #findSerieByTransmision} pero acotada al período que pide
     * el termómetro (RF-40). Ambos extremos son inclusivos y <b>obligatorios</b>:
     * cuando el período viene abierto, el servicio pasa cotas que lo cubren
     * todo. Se evita así el patrón {@code (:desde is null or ...)}, que obliga a
     * Hibernate a inferir el tipo de un parámetro nulo y necesita un cast
     * explícito para hacerlo de forma confiable.
     */
    @Query("""
            select a from AnalisisSentimiento a
            join a.metricaChat m
            where m.transmisionTwitch.id = :transmisionId
              and a.fechaHora >= :desde
              and a.fechaHora <= :hasta
            order by a.fechaHora asc, a.id asc""")
    List<AnalisisSentimiento> findSerieByTransmisionEntre(@Param("transmisionId") Long transmisionId,
                                                          @Param("desde") LocalDateTime desde,
                                                          @Param("hasta") LocalDateTime hasta);

    /**
     * Transmisiones que ya tienen análisis, para el selector del termómetro
     * (RF-40). Las más recientemente analizadas primero. El join al torneo es
     * explícitamente {@code left}: una transmisión sin torneo asociado no debe
     * desaparecer de la lista.
     */
    @Query("""
            select new com.coffeecommits.brakket.analytics.dto.TransmisionAnalizadaResponse(
                       t.id, t.estado, t.iniciadaEn, tor.id, count(a))
            from AnalisisSentimiento a
            join a.metricaChat m
            join m.transmisionTwitch t
            left join t.torneo tor
            group by t.id, t.estado, t.iniciadaEn, tor.id
            order by max(a.fechaHora) desc""")
    List<TransmisionAnalizadaResponse> findTransmisionesAnalizadas();
}
