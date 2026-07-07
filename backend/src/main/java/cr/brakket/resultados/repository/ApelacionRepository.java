package cr.brakket.resultados.repository;

import cr.brakket.resultados.domain.Apelacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApelacionRepository extends JpaRepository<Apelacion, Long> {
}
