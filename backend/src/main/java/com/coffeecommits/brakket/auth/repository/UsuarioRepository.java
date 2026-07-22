package com.coffeecommits.brakket.auth.repository;

import com.coffeecommits.brakket.auth.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByGoogleId(String googleId);

    Optional<Usuario> findByCorreo(String correo);

    /**
     * Jugadores disponibles para invitar a un equipo. Todos los filtros van en
     * la query para paginar bien y evitar el N+1 que tenia el servicio:
     * <ul>
     *   <li>solo perfiles publicos y no bloqueados;</li>
     *   <li>coincidencia por nombre (o todos si el texto viene vacio);</li>
     *   <li>coincidencia por juego preferido (o cualquiera si juegoId es null);</li>
     *   <li>excluye a quien ya es miembro activo del propio equipo;</li>
     *   <li>si soloDisponibles, excluye a quien esta activo en otro equipo
     *       (esos requeririan transferencia).</li>
     * </ul>
     * El coalesce sobre el nombre evita que un usuario con nombre nulo quede
     * invisible ante el LIKE.
     */
    @Query("""
            select u from Usuario u
            where u.visibilidadPerfil = com.coffeecommits.brakket.auth.model.VisibilidadPerfil.PUBLIC
              and u.bloqueado = false
              and (:texto = '' or lower(coalesce(u.nombre, '')) like lower(concat('%', :texto, '%')))
              and (:juegoId is null or exists (
                    select 1 from u.juegosPreferidos j where j.id = :juegoId))
              and not exists (
                    select 1 from MiembroEquipo ya
                    where ya.usuario = u and ya.estado = 'ACTIVO' and ya.equipo.id = :equipoId)
              and (:soloDisponibles = false or not exists (
                    select 1 from MiembroEquipo otro
                    where otro.usuario = u and otro.estado = 'ACTIVO' and otro.equipo.id <> :equipoId))
            """)
    Page<Usuario> buscarDisponibles(@Param("texto") String texto,
                                    @Param("juegoId") Long juegoId,
                                    @Param("equipoId") Long equipoId,
                                    @Param("soloDisponibles") boolean soloDisponibles,
                                    Pageable pageable);
}
