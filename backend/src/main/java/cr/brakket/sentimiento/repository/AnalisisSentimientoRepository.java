package cr.brakket.sentimiento.repository;

import cr.brakket.sentimiento.domain.AnalisisSentimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalisisSentimientoRepository extends JpaRepository<AnalisisSentimiento, Long> {

    List<AnalisisSentimiento> findByMetricaChatId(Long metricaChatId);
}
