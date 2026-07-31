package com.coffeecommits.brakket.notification.repository;

import com.coffeecommits.brakket.notification.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByUsuarioIdAndEliminadaBandejaFalseOrderByFechaDesc(Long usuarioId);

    List<Notificacion> findByUsuarioIdAndEliminadaBandejaFalseOrderByFechaDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalseAndEliminadaBandejaFalse(Long usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndTipoAndEntidadAndEntidadIdAndMensajeAndEliminadaBandejaFalse(
            Long usuarioId,
            String tipo,
            String entidad,
            Long entidadId,
            String mensaje);
}
