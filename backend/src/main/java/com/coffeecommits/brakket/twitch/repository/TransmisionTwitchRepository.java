package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TransmisionTwitchRepository extends JpaRepository<TransmisionTwitch, Long> {

    /**
     * Transmisiones activas con canal y torneo ya cargados: el servicio de
     * /transmisiones las lee fuera de una transacción (su resultado se cachea
     * en memoria), así que el fetch join evita LazyInitializationException.
     */
    @Query("select t from TransmisionTwitch t left join fetch t.canal left join fetch t.torneo where t.activa = true")
    List<TransmisionTwitch> findActivasConCanalYTorneo();

    /**
     * Transmisiones con período de captura abierto (RF-36): registradas,
     * activas y aún sin cerrar. El muestreador las consulta cada tick.
     */
    @Query("""
            select t from TransmisionTwitch t left join fetch t.canal
            where t.activa = true and t.finalizadaEn is null
            order by t.id""")
    List<TransmisionTwitch> findAbiertasParaMuestreo();

    // RF-44: transmisiones de un torneo para agregar sus metricas en el panel
    // comercial del patrocinador. Un torneo puede tener varias transmisiones
    // (varias jornadas); se devuelven ordenadas por fecha de inicio descendente
    // para que el servicio elija la mas relevante (en vivo, o la mas reciente).
    List<TransmisionTwitch> findByTorneoIdOrderByIniciadaEnDesc(Long torneoId);

    /** Transmisiones activas: métrica del panel global (RF-49). */
    long countByActivaTrue();

    /**
     * RF-34: transmisión abierta que ya sigue este directo. Sirve para dar un
     * error entendible antes de chocar contra el índice único.
     */
    Optional<TransmisionTwitch> findByTwitchStreamIdAndFinalizadaEnIsNull(String twitchStreamId);

    /**
     * RF-37: catálogo del panel analítico. Trae también el torneo de la partida
     * porque una transmisión puede colgar de la partida y no del torneo, y de ahí
     * sale tanto la etiqueta como el criterio de acceso del comisionado.
     */
    @Query("""
            select t from TransmisionTwitch t
            left join fetch t.torneo
            left join fetch t.partida p
            left join fetch p.torneo
            where t.activa = true
            order by t.iniciadaEn desc nulls last, t.id desc""")
    List<TransmisionTwitch> findParaAnalitica();
}
