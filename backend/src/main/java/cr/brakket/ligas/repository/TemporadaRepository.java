package cr.brakket.ligas.repository;

import cr.brakket.ligas.domain.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findByLigaId(Long ligaId);
}
