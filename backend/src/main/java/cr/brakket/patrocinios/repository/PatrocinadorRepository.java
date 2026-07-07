package cr.brakket.patrocinios.repository;

import cr.brakket.patrocinios.domain.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Long> {
}
