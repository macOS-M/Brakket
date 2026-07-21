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
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.transfer.dto.CrearTransferenciaRequest;
import com.coffeecommits.brakket.transfer.dto.TransferenciaResponse;
import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;
import com.coffeecommits.brakket.transfer.repository.SolicitudTransferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TransferRequestServiceImpl implements TransferRequestService {

    /**
     * Cupo máximo de plantilla. Provisional hasta que el catálogo de juegos o
     * las reglas de torneo definan límites propios (los criterios de RF-12
     * piden respetar "límites de plantilla cuando apliquen").
     */
    static final int CUPO_MAXIMO_PLANTILLA = 10;

    private static final Set<String> ROLES_VALIDOS = Set.of("TITULAR", "SUPLENTE", "COACH");

    private final SolicitudTransferenciaRepository transferenciaRepository;
    private final EquipoRepository equipoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    public TransferRequestServiceImpl(SolicitudTransferenciaRepository transferenciaRepository,
                                      EquipoRepository equipoRepository,
                                      MiembroEquipoRepository miembroEquipoRepository,
                                      UsuarioRepository usuarioRepository,
                                      NotificacionRepository notificacionRepository) {
        this.transferenciaRepository = transferenciaRepository;
        this.equipoRepository = equipoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    @Transactional
    public TransferenciaResponse solicitar(String solicitanteCorreo, CrearTransferenciaRequest request) {
        Usuario solicitante = usuarioRepository.findByCorreo(solicitanteCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", solicitanteCorreo));

        Equipo destino = equipoRepository.findById(request.equipoDestinoId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipo destino", request.equipoDestinoId()));
        Equipo origen = equipoRepository.findById(request.equipoOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipo origen", request.equipoOrigenId()));
        Usuario jugador = usuarioRepository.findById(request.jugadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Jugador", request.jugadorId()));

        if (!destino.getCapitan().getId().equals(solicitante.getId())) {
            throw new ForbiddenException(
                    "Solo el capitán del equipo destino puede solicitar una transferencia");
        }
        if (origen.getId().equals(destino.getId())) {
            throw new IllegalArgumentException("El equipo de origen y el destino no pueden ser el mismo");
        }
        validarEquipoOperable(destino, "destino");
        validarEquipoOperable(origen, "origen");

        String rol = request.rolPropuesto() == null ? "" : request.rolPropuesto().trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rol)) {
            throw new IllegalArgumentException(
                    "Rol propuesto inválido '%s'. Los roles permitidos son: %s"
                            .formatted(request.rolPropuesto(), ROLES_VALIDOS));
        }

        MiembroEquipo miembroOrigen = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(origen.getId(), jugador.getId())
                .filter(m -> "ACTIVO".equals(m.getEstado()))
                .orElseThrow(() -> new BusinessException(
                        "El jugador no pertenece al equipo de origen indicado"));
        if ("CAPITAN".equals(miembroOrigen.getRol())) {
            throw new BusinessException(
                    "El capitán del equipo de origen no es transferible; primero debe ceder la capitanía");
        }
        boolean yaEnDestino = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(destino.getId(), jugador.getId())
                .filter(m -> "ACTIVO".equals(m.getEstado()))
                .isPresent();
        if (yaEnDestino) {
            throw new BusinessException("El jugador ya forma parte del equipo destino");
        }

        long activosDestino = miembroEquipoRepository
                .findByEquipoIdAndEstado(destino.getId(), "ACTIVO").size();
        if (activosDestino >= CUPO_MAXIMO_PLANTILLA) {
            throw new BusinessException(
                    "El equipo destino no tiene cupo disponible (máximo %d integrantes)"
                            .formatted(CUPO_MAXIMO_PLANTILLA));
        }

        if (transferenciaRepository.existsByJugadorIdAndEstado(
                jugador.getId(), SolicitudTransferencia.ESTADO_PENDIENTE)) {
            throw new BusinessException(
                    "Ya existe una transferencia pendiente para este jugador");
        }

        SolicitudTransferencia solicitud = transferenciaRepository.save(
                SolicitudTransferencia.builder()
                        .jugador(jugador)
                        .equipoOrigen(origen)
                        .equipoDestino(destino)
                        .solicitante(solicitante)
                        .rolPropuesto(rol)
                        .justificacion(normalizar(request.justificacion()))
                        .build());

        // Notificar a las partes que deben responder (RF-12). Si algo fallara
        // aquí la transacción completa se revierte y la solicitud no queda a
        // medias; la solicitud siempre es visible en el panel de pendientes.
        notificar(jugador, solicitud,
                "El equipo %s solicita tu transferencia desde %s. Revisa tus solicitudes pendientes."
                        .formatted(destino.getNombre(), origen.getNombre()));
        notificar(origen.getCapitan(), solicitud,
                "El equipo %s solicita la transferencia de %s desde tu equipo %s."
                        .formatted(destino.getNombre(), jugador.getNombre(), origen.getNombre()));

        return TransferenciaResponse.fromEntity(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferenciaResponse> listarEnviadas(String solicitanteCorreo) {
        Usuario solicitante = usuarioRepository.findByCorreo(solicitanteCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", solicitanteCorreo));
        return transferenciaRepository
                .findBySolicitanteIdOrderByFechaSolicitudDesc(solicitante.getId())
                .stream()
                .map(TransferenciaResponse::fromEntity)
                .toList();
    }

    private void validarEquipoOperable(Equipo equipo, String etiqueta) {
        if ("DISUELTO".equals(equipo.getEstado())) {
            throw new BusinessException(
                    "El equipo %s '%s' está disuelto; no admite transferencias"
                            .formatted(etiqueta, equipo.getNombre()));
        }
        if ("BLOQUEADO".equals(equipo.getEstado())) {
            throw new BusinessException(
                    "El equipo %s '%s' está bloqueado por revisión administrativa o disputa activa"
                            .formatted(etiqueta, equipo.getNombre()));
        }
    }

    private void notificar(Usuario destinatario, SolicitudTransferencia solicitud, String mensaje) {
        notificacionRepository.save(Notificacion.builder()
                .usuario(destinatario)
                .tipo("TRANSFERENCIA_SOLICITADA")
                .mensaje(mensaje)
                .entidad("solicitud_transferencia")
                .entidadId(solicitud.getId())
                .leida(false)
                .fecha(LocalDateTime.now())
                .build());
    }

    private String normalizar(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
