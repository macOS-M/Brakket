package com.coffeecommits.brakket.auth.repository;

import com.coffeecommits.brakket.auth.model.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    List<UsuarioRol> findByUsuarioId(Long usuarioId);
}
