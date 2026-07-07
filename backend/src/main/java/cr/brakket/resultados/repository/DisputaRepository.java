package cr.brakket.resultados.repository;

import cr.brakket.resultados.domain.Disputa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    List<Disputa> findByEstado(String estado);
}
