package com.coffeecommits.brakket.notification.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.notification.model.Notificacion;
import com.coffeecommits.brakket.notification.repository.NotificacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificacionRepository notificacionRepository;

    public NotificationServiceImpl(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public void notificar(Usuario destinatario, String tipo, String mensaje, String entidad, Long entidadId) {
        Notificacion notificacion = Notificacion.builder()
                .usuario(destinatario)
                .tipo(tipo)
                .mensaje(mensaje)
                .entidad(entidad)
                .entidadId(entidadId)
                .leida(false)
                .fecha(LocalDateTime.now())
                .build();
        notificacionRepository.save(notificacion);
    }
}