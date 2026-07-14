package com.coffeecommits.brakket.auth.repository;

import com.coffeecommits.brakket.auth.model.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    List<UsuarioRol> findByUsuarioId(Long usuarioId);

    /**
     * Asignaciones del usuario con rol y permisos ya cargados en una sola
     * query (evita el N+1 al construir las authorities en cada request).
     */
    @Query("""
            select distinct ur from UsuarioRol ur
            join fetch ur.rol r
            left join fetch r.permisos
            where ur.usuario.id = :usuarioId""")
    List<UsuarioRol> findConRolYPermisosByUsuarioId(@Param("usuarioId") Long usuarioId);

    /** ¿Este usuario ya tiene este rol? (para no asignarlo dos veces) */
    boolean existsByUsuarioIdAndRolId(Long usuarioId, Long rolId);

    /** La fila exacta usuario–rol (para poder revocarla). */
    Optional<UsuarioRol> findByUsuarioIdAndRolId(Long usuarioId, Long rolId);

    /**
     * Cuántas asignaciones vigentes otorgan el permiso indicado. Se usa para
     * impedir que se revoque al último usuario capaz de gestionar roles.
     */
    @Query("""
            select count(ur) from UsuarioRol ur
            join ur.rol.permisos p
            where p.codigo = :codigo""")
    long contarAsignacionesConPermiso(@Param("codigo") String codigo);
}