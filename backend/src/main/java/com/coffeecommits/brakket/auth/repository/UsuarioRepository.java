package com.coffeecommits.brakket.auth.repository;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByGoogleId(String googleId);

    Optional<Usuario> findByCorreo(String correo);

    /** Cuentas bloqueadas: métrica de moderación del panel global (RF-49). */
    long countByBloqueadoTrue();

    /**
     * Jugadores disponibles para invitar a un equipo. Todos los filtros van en
     * la query para paginar bien y evitar el N+1 que tenia el servicio:
     * <ul>
     *   <li>solo perfiles publicos y no bloqueados;</li>
     *   <li>coincidencia por nombre (o todos si el texto viene vacio);</li>
     *   <li>coincidencia por juego preferido (o cualquiera si juegoId es null);</li>
     *   <li>excluye a quien ya es miembro activo del propio equipo;</li>
     *   <li>excluye a los administradores: moderan la plataforma, no
     *       forman parte de equipos;</li>
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
              and not exists (
                    select 1 from UsuarioRol adm
                    where adm.usuario = u and adm.rol.nombreRol = 'ADMIN')
              and (:soloDisponibles = false or not exists (
                    select 1 from MiembroEquipo otro
                    where otro.usuario = u and otro.estado = 'ACTIVO' and otro.equipo.id <> :equipoId))
            """)
    Page<Usuario> buscarDisponibles(@Param("texto") String texto,
                                    @Param("juegoId") Long juegoId,
                                    @Param("equipoId") Long equipoId,
                                    @Param("soloDisponibles") boolean soloDisponibles,
                                    Pageable pageable);

    @Query("""
            select new com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse(u.id, u.nombre)
            from Usuario u
            where lower(u.nombre) like lower(concat('%', :texto, '%'))
              and exists (
                select m.id from MiembroEquipo m, Partida p
                where m.usuario = u
                  and (p.equipoA = m.equipo or p.equipoB = m.equipo)
                  and p.estado = com.coffeecommits.brakket.tournament.model.EstadoPartida.FINALIZADA
                  and p.equipoA is not null and p.equipoB is not null
                  and (:juegoId is null or p.torneo.juego.id = :juegoId)
              )
            order by u.nombre
            """)
    Page<OpcionEstadisticaResponse> buscarOpcionesEstadisticas(@Param("texto") String texto,
                                                               @Param("juegoId") Long juegoId,
                                                               Pageable pageable);
}
