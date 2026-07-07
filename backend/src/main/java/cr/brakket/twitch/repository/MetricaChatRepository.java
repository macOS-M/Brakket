package cr.brakket.twitch.repository;

import cr.brakket.twitch.domain.MetricaChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricaChatRepository extends JpaRepository<MetricaChat, Long> {

    List<MetricaChat> findByCuentaTwitchId(Long cuentaTwitchId);
}
