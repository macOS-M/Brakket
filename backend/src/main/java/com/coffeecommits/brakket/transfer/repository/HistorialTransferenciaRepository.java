package com.coffeecommits.brakket.transfer.repository;

import com.coffeecommits.brakket.transfer.model.HistorialTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialTransferenciaRepository extends JpaRepository<HistorialTransferencia, Long> {

    /** Ya existe un registro para esta solicitud (evita duplicados, RF-14). */
    boolean existsBySolicitudId(Long solicitudId);

    /**
     * Historial de un equipo: transferencias donde participó como origen
     * o como destino (RF-16). Ordenado del más reciente al más antiguo.
     */
    List<HistorialTransferencia> findByEquipoOrigenIdOrEquipoDestinoIdOrderByFechaTransferenciaDesc(
            Long equipoOrigenId, Long equipoDestinoId);
}