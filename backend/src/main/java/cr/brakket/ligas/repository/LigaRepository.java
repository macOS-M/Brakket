package cr.brakket.ligas.repository;

import cr.brakket.ligas.domain.Liga;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigaRepository extends JpaRepository<Liga, Long> {
}
