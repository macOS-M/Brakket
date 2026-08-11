package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * La búsqueda de equipos (RF-05) usa {@link JpaSpecificationExecutor}: los
 * filtros son opcionales y la consulta se arma solo con los presentes (ver
 * {@code TeamSearchServiceImpl}).
 */
public interface EquipoRepository extends JpaRepository<Equipo, Long>,
        JpaSpecificationExecutor<Equipo> {

    Optional<Equipo> findByNombre(String nombre);

    /** Búsqueda case-insensitive: "Fnatic" y "fnatic" cuentan como el mismo nombre. */
    Optional<Equipo> findByNombreIgnoreCase(String nombre);

    /**
     * Listado público por nombre con el juego ya cargado en la misma query
     * (el resumen siempre muestra el nombre del juego).
     */
    @Query("""
            select distinct e from Equipo e
            left join fetch e.juego
            left join fetch e.juegos j
            where lower(e.nombre) like lower(concat('%', :criterio, '%'))
               or lower(j.nombre) like lower(concat('%', :criterio, '%'))
            order by e.nombre""")
    List<Equipo> buscarPorNombreConJuego(@Param("criterio") String criterio);

    @Query("""
            select distinct new com.coffeecommits.brakket.statistics.dto.OpcionEstadisticaResponse(e.id, e.nombre)
            from Equipo e left join e.juegos j
            where lower(e.nombre) like lower(concat('%', :texto, '%'))
              and (:juegoId is null or j.id = :juegoId)
              and exists (
                select p.id from Partida p
                where p.estado = com.coffeecommits.brakket.tournament.model.EstadoPartida.FINALIZADA
                  and p.equipoA is not null and p.equipoB is not null
                  and (p.equipoA = e or p.equipoB = e)
              )
            order by e.nombre
            """)
    Page<OpcionEstadisticaResponse> buscarOpcionesEstadisticas(@Param("texto") String texto,
                                                               @Param("juegoId") Long juegoId,
                                                               Pageable pageable);
}
