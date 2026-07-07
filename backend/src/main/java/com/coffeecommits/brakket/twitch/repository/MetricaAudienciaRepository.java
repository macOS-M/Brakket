package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.MetricaAudiencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricaAudienciaRepository extends JpaRepository<MetricaAudiencia, Long> {

    List<MetricaAudiencia> findByCuentaTwitchId(Long cuentaTwitchId);
}
