package com.coffeecommits.brakket.notification.repository;

import com.coffeecommits.brakket.notification.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);
}
