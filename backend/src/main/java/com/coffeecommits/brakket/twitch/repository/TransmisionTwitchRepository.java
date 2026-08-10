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

    // RF-44: resolver la transmision de un torneo para agregar sus metricas
    // en el panel comercial del patrocinador.
    Optional<TransmisionTwitch> findByTorneoId(Long torneoId);

    /** Transmisiones activas: métrica del panel global (RF-49). */
    long countByActivaTrue();
}