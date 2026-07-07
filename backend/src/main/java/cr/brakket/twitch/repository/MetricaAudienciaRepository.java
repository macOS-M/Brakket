package cr.brakket.twitch.repository;

import cr.brakket.twitch.domain.MetricaAudiencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricaAudienciaRepository extends JpaRepository<MetricaAudiencia, Long> {

    List<MetricaAudiencia> findByCuentaTwitchId(Long cuentaTwitchId);
}
