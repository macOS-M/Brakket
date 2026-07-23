package com.coffeecommits.brakket.twitch.repository;

import com.coffeecommits.brakket.twitch.model.CanalOficialTwitch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CanalOficialTwitchRepository extends JpaRepository<CanalOficialTwitch, Long> {
    Optional<CanalOficialTwitch> findFirstByActivoTrue();
    Optional<CanalOficialTwitch> findFirstByOrderByIdDesc();
    boolean existsByLoginCanalIgnoreCaseAndActivoTrue(String loginCanal);
}
