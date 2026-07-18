package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.notification.model.Notificacion;
import com.coffeecommits.brakket.notification.repository.NotificacionRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.transfer.dto.ResponderTransferenciaRequest;
import com.coffeecommits.brakket.transfer.dto.TransferenciaResponse;
import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;
import com.coffeecommits.brakket.transfer.repository.SolicitudTransferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.coffeecommits.brakket.transfer.model.SolicitudTransferencia.APROBACION_ACEPTADA;
import static com.coffeecommits.brakket.transfer.model.SolicitudTransferencia.APROBACION_PENDIENTE;
import static com.coffeecommits.brakket.transfer.model.SolicitudTransferencia.APROBACION_RECHAZADA;
import static com.coffeecommits.brakket.transfer.model.SolicitudTransferencia.ESTADO_APROBADA;
import static com.coffeecommits.brakket.transfer.model.SolicitudTransferencia.ESTADO_PENDIENTE;
import static com.coffeecommits.brakket.transfer.model.SolicitudTransferencia.ESTADO_RECHAZADA;
import static com.coffeecommits.brakket.transfer.service.TransferRequestServiceImpl.CUPO_MAXIMO_PLANTILLA;

@Service
public class TransferResponseServiceImpl implements TransferResponseService {

    private final SolicitudTransferenciaRepository transferenciaRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    public TransferResponseServiceImpl(SolicitudTransferenciaRepository transferenciaRepository,
                                       MiembroEquipoRepository miembroEquipoRepository,
                                       UsuarioRepository usuarioRepository,
                                       NotificacionRepository notificacionRepository) {
        this.transferenciaRepository = transferenciaRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferenciaResponse> listarPendientes(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        return transferenciaRepository.findPendientesParaUsuario(usuario.getId())
                .stream()
                .map(TransferenciaResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public TransferenciaResponse responder(String correo, Long solicitudId,
                                           ResponderTransferenciaRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        SolicitudTransferencia solicitud = transferenciaRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de transferencia", solicitudId));

        if (!ESTADO_PENDIENTE.equals(solicitud.getEstado())) {
            throw new BusinessException("La solicitud ya fue resuelta (estado: %s)"
                    .formatted(solicitud.getEstado()));
        }

        boolean esJugador = solicitud.getJugador().getId().equals(usuario.getId());
        boolean esCapitanOrigen = solicitud.getEquipoOrigen().getCapitan().getId()
                .equals(usuario.getId());
        if (!esJugador && !esCapitanOrigen) {
            throw new ForbiddenException(
                    "Solo el jugador o el capitán del equipo de origen pueden responder esta solicitud");
        }

        String aprobacionActual = esJugador
                ? solicitud.getAprobacionJugador()
                : solicitud.getAprobacionCapitanOrigen();
        if (!APROBACION_PENDIENTE.equals(aprobacionActual)) {
            throw new BusinessException("Ya registraste tu respuesta a esta solicitud");
        }

        if (request.esRechazo()) {
            registrarAprobacion(solicitud, esJugador, APROBACION_RECHAZADA);
            resolver(solicitud, ESTADO_RECHAZADA, usuario);
            notificarPartes(solicitud, "TRANSFERENCIA_RECHAZADA",
                    "La transferencia de %s hacia %s fue rechazada por %s."
                            .formatted(solicitud.getJugador().getNombre(),
                                    solicitud.getEquipoDestino().getNombre(),
                                    usuario.getNombre()));
        } else {
            registrarAprobacion(solicitud, esJugador, APROBACION_ACEPTADA);
            if (APROBACION_ACEPTADA.equals(solicitud.getAprobacionJugador())
                    && APROBACION_ACEPTADA.equals(solicitud.getAprobacionCapitanOrigen())) {
                ejecutarTransferencia(solicitud);
                resolver(solicitud, ESTADO_APROBADA, usuario);
                notificarPartes(solicitud, "TRANSFERENCIA_APROBADA",
                        "Transferencia completada: %s pasa de %s a %s."
                                .formatted(solicitud.getJugador().getNombre(),
                                        solicitud.getEquipoOrigen().getNombre(),
                                        solicitud.getEquipoDestino().getNombre()));
            } else {
                // Aceptación parcial: avisar al solicitante que falta la otra parte.
                notificar(solicitud.getSolicitante(), solicitud, "TRANSFERENCIA_ACTUALIZADA",
                        "%s aceptó la transferencia de %s; falta la otra aprobación."
                                .formatted(usuario.getNombre(), solicitud.getJugador().getNombre()));
            }
        }

        return TransferenciaResponse.fromEntity(transferenciaRepository.save(solicitud));
    }

    private void registrarAprobacion(SolicitudTransferencia solicitud, boolean esJugador, String valor) {
        if (esJugador) {
            solicitud.setAprobacionJugador(valor);
        } else {
            solicitud.setAprobacionCapitanOrigen(valor);
        }
    }

    private void resolver(SolicitudTransferencia solicitud, String estado, Usuario resueltaPor) {
        solicitud.setEstado(estado);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setResueltaPor(resueltaPor);
    }

    /**
     * Ejecuta el movimiento de plantilla con las reglas revalidadas al momento
     * de completarse (criterio RF-13). Todo dentro de la misma transacción:
     * si algo falla, la plantilla no queda modificada parcialmente.
     */
    private void ejecutarTransferencia(SolicitudTransferencia solicitud) {
        Equipo origen = solicitud.getEquipoOrigen();
        Equipo destino = solicitud.getEquipoDestino();
        Usuario jugador = solicitud.getJugador();

        validarEquipoOperable(origen);
        validarEquipoOperable(destino);

        MiembroEquipo miembroOrigen = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(origen.getId(), jugador.getId())
                .filter(m -> "ACTIVO".equals(m.getEstado()))
                .orElseThrow(() -> new BusinessException(
                        "El jugador ya no pertenece al equipo de origen; la transferencia no puede completarse"));

        long activosDestino = miembroEquipoRepository
                .findByEquipoIdAndEstado(destino.getId(), "ACTIVO").size();
        if (activosDestino >= CUPO_MAXIMO_PLANTILLA) {
            throw new BusinessException(
                    "El equipo destino ya no tiene cupo disponible; la transferencia no puede completarse");
        }

        // Baja lógica en el origen: se conserva el historial del paso por el equipo.
        miembroOrigen.setEstado("TRANSFERIDO");
        miembroEquipoRepository.save(miembroOrigen);

        // Alta en el destino: si hubo un paso previo por el equipo se reactiva esa
        // fila (equipo+usuario es único); si no, se crea una nueva.
        MiembroEquipo miembroDestino = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(destino.getId(), jugador.getId())
                .orElseGet(() -> MiembroEquipo.builder()
                        .equipo(destino)
                        .usuario(jugador)
                        .build());
        miembroDestino.setEstado("ACTIVO");
        miembroDestino.setRol(solicitud.getRolPropuesto());
        miembroDestino.setFechaUnion(LocalDate.now());
        miembroEquipoRepository.save(miembroDestino);
    }

    private void validarEquipoOperable(Equipo equipo) {
        if ("DISUELTO".equals(equipo.getEstado()) || "BLOQUEADO".equals(equipo.getEstado())) {
            throw new BusinessException(
                    "El equipo '%s' está %s; la transferencia no puede completarse"
                            .formatted(equipo.getNombre(), equipo.getEstado().toLowerCase()));
        }
    }

    /** Notifica al jugador, al capitán de origen y al solicitante (capitán destino). */
    private void notificarPartes(SolicitudTransferencia solicitud, String tipo, String mensaje) {
        notificar(solicitud.getJugador(), solicitud, tipo, mensaje);
        notificar(solicitud.getEquipoOrigen().getCapitan(), solicitud, tipo, mensaje);
        notificar(solicitud.getSolicitante(), solicitud, tipo, mensaje);
    }

    private void notificar(Usuario destinatario, SolicitudTransferencia solicitud,
                           String tipo, String mensaje) {
        notificacionRepository.save(Notificacion.builder()
                .usuario(destinatario)
                .tipo(tipo)
                .mensaje(mensaje)
                .entidad("solicitud_transferencia")
                .entidadId(solicitud.getId())
                .leida(false)
                .fecha(LocalDateTime.now())
                .build());
    }
}
