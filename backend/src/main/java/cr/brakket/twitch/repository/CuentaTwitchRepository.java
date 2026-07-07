package cr.brakket.twitch.repository;

import cr.brakket.twitch.domain.CuentaTwitch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaTwitchRepository extends JpaRepository<CuentaTwitch, Long> {

    List<CuentaTwitch> findByEquipoId(Long equipoId);
}
