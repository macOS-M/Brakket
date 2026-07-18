package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            select e from Equipo e
            left join fetch e.juego
            where lower(e.nombre) like lower(concat('%', :criterio, '%'))
            order by e.nombre""")
    List<Equipo> buscarPorNombreConJuego(@Param("criterio") String criterio);
}
