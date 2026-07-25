package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.CuentaTwitch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaTwitchRepository extends JpaRepository<CuentaTwitch, Long> {

    List<CuentaTwitch> findByEquipoId(Long equipoId);
}
