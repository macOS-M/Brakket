package com.coffeecommits.brakket.auth.repository;

import com.coffeecommits.brakket.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByGoogleId(String googleId);

    Optional<Usuario> findByCorreo(String correo);

    @Query("""
            select u from Usuario u
            where u.visibilidadPerfil = com.coffeecommits.brakket.auth.model.VisibilidadPerfil.PUBLIC
              and u.bloqueado = false
              and lower(u.nombre) like lower(concat('%', :texto, '%'))
              and (:juegoId is null or exists (
                    select 1 from u.juegosPreferidos j where j.id = :juegoId))
            """)
    List<Usuario> buscarDisponibles(@Param("texto") String texto, @Param("juegoId") Long juegoId);
}