package com.coffeecommits.brakket.progression.repository;
import com.coffeecommits.brakket.progression.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PersonalizacionPerfilRepository extends JpaRepository<PersonalizacionPerfil, PersonalizacionPerfilId> {
    List<PersonalizacionPerfil> findByUsuarioId(Long usuarioId);
}
