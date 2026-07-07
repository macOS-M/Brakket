package com.coffeecommits.brakket.auth.repository;

import com.coffeecommits.brakket.auth.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombreRol(String nombreRol);
}
