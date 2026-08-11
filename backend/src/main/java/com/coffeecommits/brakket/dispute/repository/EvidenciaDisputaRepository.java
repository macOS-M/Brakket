package com.coffeecommits.brakket.dispute.repository;

import com.coffeecommits.brakket.dispute.model.EvidenciaDisputa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenciaDisputaRepository extends JpaRepository<EvidenciaDisputa, Long> {

    List<EvidenciaDisputa> findByDisputaIdOrderByFechaCreacionAsc(Long disputaId);
}