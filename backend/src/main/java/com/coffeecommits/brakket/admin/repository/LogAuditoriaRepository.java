package com.coffeecommits.brakket.admin.repository;

import com.coffeecommits.brakket.admin.model.LogAuditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    /**
     * Últimas entradas de auditoría (más recientes primero) para la actividad
     * del panel global (RF-49). El límite se controla con el {@link Pageable}.
     */
    List<LogAuditoria> findAllByOrderByFechaDescIdDesc(Pageable pageable);
}
