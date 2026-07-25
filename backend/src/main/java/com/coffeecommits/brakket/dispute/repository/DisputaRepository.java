package com.coffeecommits.brakket.dispute.repository;

import com.coffeecommits.brakket.dispute.model.Disputa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    List<Disputa> findByEstado(String estado);
}
