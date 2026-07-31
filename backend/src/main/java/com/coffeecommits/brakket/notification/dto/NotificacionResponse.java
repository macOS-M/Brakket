package com.coffeecommits.brakket.notification.dto;

import com.coffeecommits.brakket.notification.model.EstadoEntrega;
import com.coffeecommits.brakket.notification.model.Notificacion;
import com.coffeecommits.brakket.notification.model.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        TipoNotificacion tipo,
        String mensaje,
        String origen,
        String entidad,
        Long entidadId,
        boolean leida,
        LocalDateTime fecha,
        EstadoEntrega estadoEntrega) {

    public static NotificacionResponse from(Notificacion n) {
        TipoNotificacion tipo;
        try {
            tipo = TipoNotificacion.valueOf(n.getTipo());
        } catch (IllegalArgumentException | NullPointerException ignored) {
            tipo = TipoNotificacion.ADMINISTRATIVA;
        }
        return new NotificacionResponse(n.getId(), tipo, n.getMensaje(), n.getOrigen(),
                n.getEntidad(), n.getEntidadId(), Boolean.TRUE.equals(n.getLeida()), n.getFecha(),
                n.getEstadoEntrega());
    }
}
