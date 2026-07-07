package cr.brakket.patrocinios.repository;

import cr.brakket.patrocinios.domain.ReportePatrocinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportePatrocinadorRepository extends JpaRepository<ReportePatrocinador, Long> {

    List<ReportePatrocinador> findByPatrocinadorId(Long patrocinadorId);
}
