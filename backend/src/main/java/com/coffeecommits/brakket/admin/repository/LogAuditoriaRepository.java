package com.coffeecommits.brakket.admin.repository;

import com.coffeecommits.brakket.admin.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {
}
