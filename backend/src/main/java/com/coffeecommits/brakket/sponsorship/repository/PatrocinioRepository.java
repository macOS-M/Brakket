package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrocinioRepository extends JpaRepository<Patrocinio, Long> {

    List<Patrocinio> findByTorneoId(Long torneoId);
}
