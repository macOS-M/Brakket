package com.coffeecommits.brakket.transfer.repository;

import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudTransferenciaRepository extends JpaRepository<SolicitudTransferencia, Long> {

    /** Evita solicitudes pendientes duplicadas para el mismo jugador (RF-12). */
    boolean existsByJugadorIdAndEstado(Long jugadorId, String estado);

    /** Seguimiento del equipo solicitante: todo lo pedido por este capitán. */
    List<SolicitudTransferencia> findBySolicitanteIdOrderByFechaSolicitudDesc(Long solicitanteId);

    /**
     * Bandeja de pendientes de una parte autorizada (RF-13): solicitudes
     * PENDIENTES donde el usuario es el jugador o el capitán del equipo origen.
     */
    @Query("""
            select s from SolicitudTransferencia s
            where s.estado = 'PENDIENTE'
              and (s.jugador.id = :usuarioId or s.equipoOrigen.capitan.id = :usuarioId)
            order by s.fechaSolicitud desc""")
    List<SolicitudTransferencia> findPendientesParaUsuario(@Param("usuarioId") Long usuarioId);
}
