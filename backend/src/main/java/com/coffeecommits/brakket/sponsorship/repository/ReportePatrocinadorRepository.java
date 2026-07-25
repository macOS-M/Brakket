package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.ReportePatrocinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportePatrocinadorRepository extends JpaRepository<ReportePatrocinador, Long> {

    List<ReportePatrocinador> findByPatrocinadorId(Long patrocinadorId);
}
