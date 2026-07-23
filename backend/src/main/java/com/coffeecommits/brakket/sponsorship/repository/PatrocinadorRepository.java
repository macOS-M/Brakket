package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Long> {

    Optional<Patrocinador> findByNombreIgnoreCase(String nombre);
}