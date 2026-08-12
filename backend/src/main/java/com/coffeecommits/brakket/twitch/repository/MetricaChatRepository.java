package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.MetricaChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MetricaChatRepository extends JpaRepository<MetricaChat, Long> {

    List<MetricaChat> findByCuentaTwitchId(Long cuentaTwitchId);

    /** RF-37: actividad de chat de una transmisión dentro del rango, extremos inclusivos. */
    @Query("""
            select m from MetricaChat m
            where m.transmisionTwitch.id = :transmisionId
              and m.fechaHora >= :desde and m.fechaHora <= :hasta
            order by m.fechaHora asc""")
    List<MetricaChat> buscarPorTransmisionYRango(@Param("transmisionId") Long transmisionId,
                                                 @Param("desde") LocalDateTime desde,
                                                 @Param("hasta") LocalDateTime hasta);

    // RF-44: filas de metrica_chat de una transmision especifica, usadas para
    // recorrer y agregar el sentimiento predominante via analisis_sentimiento.
    List<MetricaChat> findByTransmisionTwitchId(Long transmisionTwitchId);

    // RF-44: promedio de interaccion del chat para el panel comercial.
    @Query("""
            select count(m) as muestras, avg(m.mensajesPorMinuto) as mensajesPorMinutoPromedio
            from MetricaChat m where m.transmisionTwitch.id = :transmisionId""")
    ResumenChat resumenPorTransmision(@Param("transmisionId") Long transmisionId);

    interface ResumenChat {
        Long getMuestras();
        Double getMensajesPorMinutoPromedio();
    }
}
