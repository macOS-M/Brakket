package cr.brakket.auth.repository;

import cr.brakket.auth.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByGoogleId(String googleId);

    Optional<Usuario> findByCorreo(String correo);
}
