package com.coffeecommits.brakket.twitch.repository;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface TransmisionTwitchRepository extends JpaRepository<TransmisionTwitch, Long> {

    /**
     * Transmisiones activas con canal y torneo ya cargados: el servicio de
     * /transmisiones las lee fuera de una transacción (su resultado se cachea
     * en memoria), así que el fetch join evita LazyInitializationException.
     */
    @Query("select t from TransmisionTwitch t left join fetch t.canal left join fetch t.torneo where t.activa = true")
    List<TransmisionTwitch> findActivasConCanalYTorneo();
}
