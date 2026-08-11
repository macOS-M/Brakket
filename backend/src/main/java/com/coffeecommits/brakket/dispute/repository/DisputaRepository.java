package com.coffeecommits.brakket.dispute.repository;

import com.coffeecommits.brakket.dispute.model.Disputa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    List<Disputa> findByEstado(String estado);

    List<Disputa> findByPartidaId(Long partidaId);

    /**
     * Vista panorámica "Mis disputas" (mejora fuera de RF, no numerada):
     * todas las disputas donde el usuario es quien la levantó, el
     * organizador del torneo, un árbitro asignado a ese torneo, o el
     * comisionado de la liga de la temporada del torneo. Un admin usa
     * findAll() en su lugar (no necesita este filtro).
     */
    @Query("""
            SELECT d FROM Disputa d
            JOIN d.partida p
            JOIN p.torneo t
            WHERE d.levantadaPor.id = :usuarioId
               OR t.organizador.id = :usuarioId
               OR EXISTS (SELECT 1 FROM ArbitroTorneo at WHERE at.torneo = t AND at.usuario.id = :usuarioId)
               OR (t.temporada IS NOT NULL AND t.temporada.liga.comisionado.id = :usuarioId)
            ORDER BY d.fechaCreacion DESC
            """)
    List<Disputa> findRelevantesParaUsuario(@Param("usuarioId") Long usuarioId);
}