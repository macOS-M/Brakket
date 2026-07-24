package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Long> {

    /**
     * Lista (no Optional): "crear de todas formas" permite homónimos, así
     * que puede haber más de una fila con el mismo nombre y un Optional
     * reventaría con IncorrectResultSize en cada consulta posterior.
     */
    List<Patrocinador> findByNombreIgnoreCase(String nombre);
}