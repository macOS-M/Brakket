package cr.brakket.torneos.repository;

import cr.brakket.torneos.domain.ArbitroTorneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArbitroTorneoRepository extends JpaRepository<ArbitroTorneo, Long> {

    List<ArbitroTorneo> findByTorneoId(Long torneoId);
}
