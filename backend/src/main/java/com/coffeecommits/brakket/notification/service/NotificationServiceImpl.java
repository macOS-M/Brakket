package com.coffeecommits.brakket.notification.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.notification.dto.NotificacionResponse;
import com.coffeecommits.brakket.notification.model.EstadoEntrega;
import com.coffeecommits.brakket.notification.model.Notificacion;
import com.coffeecommits.brakket.notification.model.TipoNotificacion;
import com.coffeecommits.brakket.notification.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificationServiceImpl(NotificacionRepository notificacionRepository,
                                   UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void notificar(Usuario destinatario, String tipo, String mensaje, String entidad, Long entidadId) {
        notificar(destinatario, TipoNotificacion.valueOf(tipo), mensaje, entidad, entidad, entidadId);
    }

    @Override
    @Transactional
    public void notificar(Usuario destinatario, TipoNotificacion tipo, String mensaje,
                          String origen, String entidad, Long entidadId) {
        if (destinatario == null || tipo == null || mensaje == null || mensaje.isBlank()
                || entidad == null || entidad.isBlank() || entidadId == null) {
            throw new IllegalArgumentException("La notificación requiere destinatario, tipo, evento y motivo");
        }
        String mensajeLimpio = mensaje.trim();
        if (notificacionRepository.existsByUsuarioIdAndTipoAndEntidadAndEntidadIdAndMensaje(
                destinatario.getId(), tipo.name(), entidad, entidadId, mensajeLimpio)) {
            return;
        }
        Notificacion notificacion = Notificacion.builder()
                .usuario(destinatario)
                .tipo(tipo.name())
                .mensaje(mensajeLimpio)
                .entidad(entidad)
                .entidadId(entidadId)
                .origen(origen == null || origen.isBlank() ? entidad : origen.trim())
                .leida(false)
                .fecha(LocalDateTime.now())
                .estadoEntrega(EstadoEntrega.DISPONIBLE)
                .eliminadaBandeja(false)
                .build();
        notificacionRepository.save(notificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(String correo) {
        Usuario usuario = usuario(correo);
        return notificacionRepository
                .findByUsuarioIdAndEliminadaBandejaFalseOrderByFechaDesc(usuario.getId())
                .stream().map(NotificacionResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(String correo) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalseAndEliminadaBandejaFalse(
                usuario(correo).getId());
    }

    @Override
    @Transactional
    public NotificacionResponse marcarLeida(String correo, Long id) {
        Notificacion notificacion = propia(correo, id);
        notificacion.setLeida(true);
        return NotificacionResponse.from(notificacion);
    }

    @Override
    @Transactional
    public void marcarTodasLeidas(String correo) {
        notificacionRepository.findByUsuarioIdAndLeidaFalse(usuario(correo).getId())
                .forEach(notificacion -> notificacion.setLeida(true));
    }

    @Override
    @Transactional
    public void eliminarDeBandeja(String correo, Long id) {
        propia(correo, id).setEliminadaBandeja(true);
    }

    private Notificacion propia(String correo, Long id) {
        Usuario usuario = usuario(correo);
        return notificacionRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notificación no encontrada"));
    }

    private Usuario usuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));
    }
}
