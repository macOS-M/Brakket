package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.PatrocinadorEnlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrocinadorEnlaceRepository extends JpaRepository<PatrocinadorEnlace, Long> {

    List<PatrocinadorEnlace> findByPatrocinadorId(Long patrocinadorId);
}