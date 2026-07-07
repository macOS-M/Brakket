package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.MetricaChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricaChatRepository extends JpaRepository<MetricaChat, Long> {

    List<MetricaChat> findByCuentaTwitchId(Long cuentaTwitchId);
}
