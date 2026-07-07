package cr.brakket.torneos.repository;

import cr.brakket.torneos.domain.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    List<Torneo> findByTemporadaId(Long temporadaId);
}
