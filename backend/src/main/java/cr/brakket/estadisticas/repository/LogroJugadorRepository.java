package cr.brakket.estadisticas.repository;

import cr.brakket.estadisticas.domain.LogroJugador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogroJugadorRepository extends JpaRepository<LogroJugador, Long> {

    List<LogroJugador> findByUsuarioId(Long usuarioId);
}
