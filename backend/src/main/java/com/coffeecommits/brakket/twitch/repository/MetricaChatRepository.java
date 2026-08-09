package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.MetricaChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetricaChatRepository extends JpaRepository<MetricaChat, Long> {

    List<MetricaChat> findByCuentaTwitchId(Long cuentaTwitchId);

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