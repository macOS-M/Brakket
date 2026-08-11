package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.MetricaAudiencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MetricaAudienciaRepository extends JpaRepository<MetricaAudiencia, Long> {

    List<MetricaAudiencia> findByCuentaTwitchId(Long cuentaTwitchId);

    /**
     * Indicadores básicos de RF-36 calculados en BD sobre las muestras de una
     * transmisión. AVG/MAX ignoran filas inexistentes: sin muestras devuelve
     * conteo 0 y agregados null, que el servicio traduce a "sin datos".
     */
    @Query("""
            select count(m) as muestras, max(m.espectadores) as pico,
                   avg(m.espectadores) as promedio, max(m.fechaHora) as ultimaMuestra
            from MetricaAudiencia m where m.transmisionTwitch.id = :transmisionId""")
    ResumenAudiencia resumenPorTransmision(@Param("transmisionId") Long transmisionId);

    interface ResumenAudiencia {
        Long getMuestras();
        Integer getPico();
        Double getPromedio();
        LocalDateTime getUltimaMuestra();
    }

    /**
     * RF-37: muestras de una transmisión dentro del rango, con ambos extremos inclusivos.
     * El rango llega siempre acotado desde el servicio: un "(:desde is null or ...)" deja
     * un parámetro sin tipo que Postgres no puede inferir cuando se pasa null.
     */
    @Query("""
            select m from MetricaAudiencia m
            where m.transmisionTwitch.id = :transmisionId
              and m.fechaHora >= :desde and m.fechaHora <= :hasta
            order by m.fechaHora asc""")
    List<MetricaAudiencia> buscarPorTransmisionYRango(@Param("transmisionId") Long transmisionId,
                                                      @Param("desde") LocalDateTime desde,
                                                      @Param("hasta") LocalDateTime hasta);

    /** RF-37: cuántas muestras tiene cada transmisión, para el catálogo del panel. */
    @Query("""
            select m.transmisionTwitch.id, count(m) from MetricaAudiencia m
            where m.transmisionTwitch.id in :transmisionIds
            group by m.transmisionTwitch.id""")
    List<Object[]> contarMuestrasPorTransmision(@Param("transmisionIds") List<Long> transmisionIds);
}
