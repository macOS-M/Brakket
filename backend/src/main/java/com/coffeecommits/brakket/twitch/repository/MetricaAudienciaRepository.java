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
}
