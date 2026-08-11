package com.coffeecommits.brakket.sponsorship.repository;

import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Long> {

    /**
     * Lista (no Optional): "crear de todas formas" permite homónimos, así
     * que puede haber más de una fila con el mismo nombre y un Optional
     * reventaría con IncorrectResultSize en cada consulta posterior.
     */
    List<Patrocinador> findByNombreIgnoreCase(String nombre);

    // Resuelve la marca desde el usuario autenticado (RF-44): a diferencia
    // del nombre, usuario_id es unico por diseno (una cuenta = un patrocinador),
    // asi que aqui Optional si es seguro.
    Optional<Patrocinador> findByUsuarioId(Long usuarioId);
}