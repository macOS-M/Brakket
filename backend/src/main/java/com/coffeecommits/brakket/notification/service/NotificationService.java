package com.coffeecommits.brakket.notification.service;

import com.coffeecommits.brakket.auth.model.Usuario;

public interface NotificationService {

    void notificar(Usuario destinatario, String tipo, String mensaje, String entidad, Long entidadId);
}