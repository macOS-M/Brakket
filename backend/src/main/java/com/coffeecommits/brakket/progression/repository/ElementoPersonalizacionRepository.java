package com.coffeecommits.brakket.progression.repository;
import com.coffeecommits.brakket.progression.model.ElementoPersonalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ElementoPersonalizacionRepository extends JpaRepository<ElementoPersonalizacion, Long> {
    List<ElementoPersonalizacion> findAllByOrderByCostoPuntosAsc();
}
