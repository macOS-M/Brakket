package cr.brakket.notificaciones.repository;

import cr.brakket.notificaciones.domain.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);
}
