package com.coffeecommits.brakket.progression.repository;

import com.coffeecommits.brakket.progression.model.Logro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogroRepository extends JpaRepository<Logro, Long> {
    List<Logro> findByActivoTrue();
}
