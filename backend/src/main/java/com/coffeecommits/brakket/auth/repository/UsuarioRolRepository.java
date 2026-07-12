package com.coffeecommits.brakket.auth.repository;

import com.coffeecommits.brakket.auth.model.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    List<UsuarioRol> findByUsuarioId(Long usuarioId);

    /** ¿Este usuario ya tiene este rol? (para no asignarlo dos veces) */
    boolean existsByUsuarioIdAndRolId(Long usuarioId, Long rolId);

    /** La fila exacta usuario–rol (para poder revocarla). */
    Optional<UsuarioRol> findByUsuarioIdAndRolId(Long usuarioId, Long rolId);
}