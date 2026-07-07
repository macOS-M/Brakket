package cr.brakket.estadisticas.repository;

import cr.brakket.estadisticas.domain.EstadisticaJugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadisticaJugadorRepository extends JpaRepository<EstadisticaJugador, Long> {

    List<EstadisticaJugador> findByUsuarioId(Long usuarioId);

    Optional<EstadisticaJugador> findByUsuarioIdAndJuegoId(Long usuarioId, Long juegoId);
}
