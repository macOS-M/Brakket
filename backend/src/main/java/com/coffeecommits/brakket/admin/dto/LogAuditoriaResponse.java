package com.coffeecommits.brakket.admin.dto;

import com.coffeecommits.brakket.admin.model.LogAuditoria;

import java.time.LocalDateTime;

/**
 * Entrada del log de auditoría para la actividad reciente del panel global
 * (RF-49). El actor puede ser nulo si la acción no quedó asociada a un usuario.
 */
public record LogAuditoriaResponse(
        Long id,
        LocalDateTime fecha,
        String accion,
        String entidad,
        Long entidadId,
        String actorNombre,
        String actorCorreo
) {
    public static LogAuditoriaResponse from(LogAuditoria log) {
        var actor = log.getUsuario();
        return new LogAuditoriaResponse(
                log.getId(),
                log.getFecha(),
                log.getAccion(),
                log.getEntidad(),
                log.getEntidadId(),
                actor == null ? null : actor.getNombre(),
                actor == null ? null : actor.getCorreo()
        );
    }
}
