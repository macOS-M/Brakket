package cr.brakket.patrocinios.repository;

import cr.brakket.patrocinios.domain.Patrocinio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrocinioRepository extends JpaRepository<Patrocinio, Long> {

    List<Patrocinio> findByTorneoId(Long torneoId);
}
