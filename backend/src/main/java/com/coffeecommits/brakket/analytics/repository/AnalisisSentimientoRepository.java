package com.coffeecommits.brakket.analytics.repository;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalisisSentimientoRepository extends JpaRepository<AnalisisSentimiento, Long> {

    List<AnalisisSentimiento> findByMetricaChatId(Long metricaChatId);
}
