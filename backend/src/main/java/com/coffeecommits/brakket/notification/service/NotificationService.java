package com.coffeecommits.brakket.notification.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.notification.dto.NotificacionResponse;
import com.coffeecommits.brakket.notification.model.TipoNotificacion;

import java.util.List;

public interface NotificationService {

    void notificar(Usuario destinatario, String tipo, String mensaje, String entidad, Long entidadId);

    void notificar(Usuario destinatario, TipoNotificacion tipo, String mensaje,
                   String origen, String entidad, Long entidadId);

    List<NotificacionResponse> listar(String correo);

    long contarNoLeidas(String correo);

    NotificacionResponse marcarLeida(String correo, Long id);

    void marcarTodasLeidas(String correo);

    void eliminarDeBandeja(String correo, Long id);
}
