package com.coffeecommits.brakket.progression.repository;
import com.coffeecommits.brakket.progression.model.CanjePersonalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CanjePersonalizacionRepository extends JpaRepository<CanjePersonalizacion, Long> {
    List<CanjePersonalizacion> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndElementoId(Long usuarioId, Long elementoId);
}
