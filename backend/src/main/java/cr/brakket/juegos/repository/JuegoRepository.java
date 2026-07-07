package cr.brakket.juegos.repository;

import cr.brakket.juegos.domain.Juego;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JuegoRepository extends JpaRepository<Juego, Long> {

    Optional<Juego> findByNombre(String nombre);
}
