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
}
