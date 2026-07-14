package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.EquipoRedSocial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipoRedSocialRepository extends JpaRepository<EquipoRedSocial, Long> {

    List<EquipoRedSocial> findByEquipoId(Long equipoId);

    void deleteByEquipoId(Long equipoId);
}
