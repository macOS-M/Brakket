package com.coffeecommits.brakket.team.repository;

import com.coffeecommits.brakket.team.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    Optional<Equipo> findByNombre(String nombre);

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
