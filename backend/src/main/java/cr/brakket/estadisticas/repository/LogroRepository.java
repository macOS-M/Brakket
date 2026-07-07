package cr.brakket.estadisticas.repository;

import cr.brakket.estadisticas.domain.Logro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogroRepository extends JpaRepository<Logro, Long> {
}
