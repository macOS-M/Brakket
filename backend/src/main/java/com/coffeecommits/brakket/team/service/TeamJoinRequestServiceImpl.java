package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.notification.service.NotificationService;
import com.coffeecommits.brakket.team.dto.EquipoBusquedaResponse;
import com.coffeecommits.brakket.team.dto.SolicitarUnionRequest;
import com.coffeecommits.brakket.team.dto.SolicitudUnionResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.model.SolicitudUnion;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.team.repository.SolicitudUnionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeamJoinRequestServiceImpl implements TeamJoinRequestService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    /** Mismo tope que las invitaciones (RF-07). */
    private static final int MAX_MIEMBROS_ACTIVOS = 10;
    /** Rol de entrada al aceptar; el capitán lo ajusta después (RF-09). */
    private static final String ROL_ENTRADA = "TITULAR";

    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final SolicitudUnionRepository solicitudRepository;
    private final NotificationService notificationService;

    public TeamJoinRequestServiceImpl(EquipoRepository equipoRepository,
                                      UsuarioRepository usuarioRepository,
                                      UsuarioRolRepository usuarioRolRepository,
                                      MiembroEquipoRepository miembroEquipoRepository,
                                      SolicitudUnionRepository solicitudRepository,
                                      NotificationService notificationService) {
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.solicitudRepository = solicitudRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public SolicitudUnionResponse solicitar(Long equipoId, String correo, SolicitarUnionRequest request) {
        Usuario jugador = buscarUsuario(correo);
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        if (usuarioRolRepository.existsByUsuarioIdAndRolNombreRol(jugador.getId(), "ADMIN")) {
            throw new BusinessException(
                    "Un administrador de la plataforma no puede formar parte de equipos");
        }
        if (!ESTADO_ACTIVO.equals(equipo.getEstado())) {
            throw new BusinessException("El equipo no está activo");
        }
        miembroEquipoRepository.findByEquipoIdAndUsuarioId(equipoId, jugador.getId())
                .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                .ifPresent(m -> {
                    throw new BusinessException("Ya sos parte de este equipo");
                });
        if (solicitudRepository.existsByEquipoIdAndJugadorIdAndEstado(
                equipoId, jugador.getId(), ESTADO_PENDIENTE)) {
            throw new BusinessException("Ya tenés una solicitud pendiente con este equipo");
        }

        SolicitudUnion solicitud = solicitudRepository.save(SolicitudUnion.builder()
                .equipo(equipo)
                .jugador(jugador)
                .mensaje(normalizar(request.mensaje()))
                .estado(ESTADO_PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build());

        capitanDe(equipoId).ifPresent(capitan -> notificationService.notificar(
                capitan.getUsuario(),
                "SOLICITUD_UNION",
                "%s quiere unirse a tu equipo '%s'".formatted(jugador.getNombre(), equipo.getNombre()),
                "SolicitudUnion",
                solicitud.getId()));

        return SolicitudUnionResponse.fromEntity(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudUnionResponse> pendientesDeEquipo(Long equipoId, String correo) {
        exigirCapitan(equipoId, buscarUsuario(correo));
        return solicitudRepository
                .findByEquipoIdAndEstadoOrderByFechaCreacionAsc(equipoId, ESTADO_PENDIENTE).stream()
                .map(SolicitudUnionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public SolicitudUnionResponse responder(Long solicitudId, String correo, boolean aceptar) {
        SolicitudUnion solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud", solicitudId));
        Usuario capitan = buscarUsuario(correo);
        exigirCapitan(solicitud.getEquipo().getId(), capitan);

        if (!ESTADO_PENDIENTE.equals(solicitud.getEstado())) {
            throw new BusinessException("La solicitud ya no está pendiente");
        }

        if (aceptar) {
            Long equipoId = solicitud.getEquipo().getId();
            long activos = miembroEquipoRepository.findByEquipoId(equipoId).stream()
                    .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                    .count();
            if (activos >= MAX_MIEMBROS_ACTIVOS) {
                throw new BusinessException("El equipo no tiene cupo disponible");
            }

            // Si el jugador ya tuvo una fila (salió o fue expulsado) se
            // reactiva; si no, se crea una nueva membresía.
            MiembroEquipo miembro = miembroEquipoRepository
                    .findByEquipoIdAndUsuarioId(equipoId, solicitud.getJugador().getId())
                    .orElseGet(() -> MiembroEquipo.builder()
                            .equipo(solicitud.getEquipo())
                            .usuario(solicitud.getJugador())
                            .build());
            miembro.setEstado(ESTADO_ACTIVO);
            miembro.setRol(ROL_ENTRADA);
            miembro.setFechaUnion(LocalDate.now());
            miembroEquipoRepository.save(miembro);

            solicitud.setEstado("ACEPTADA");
            notificationService.notificar(
                    solicitud.getJugador(),
                    "SOLICITUD_ACEPTADA",
                    "Tu solicitud para unirte a '%s' fue aceptada".formatted(
                            solicitud.getEquipo().getNombre()),
                    "SolicitudUnion",
                    solicitud.getId());
        } else {
            solicitud.setEstado("RECHAZADA");
            notificationService.notificar(
                    solicitud.getJugador(),
                    "SOLICITUD_RECHAZADA",
                    "Tu solicitud para unirte a '%s' fue rechazada".formatted(
                            solicitud.getEquipo().getNombre()),
                    "SolicitudUnion",
                    solicitud.getId());
        }

        solicitud.setFechaRespuesta(LocalDateTime.now());
        return SolicitudUnionResponse.fromEntity(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoBusquedaResponse> misEquipos(String correo) {
        Usuario usuario = buscarUsuario(correo);
        return miembroEquipoRepository.findByUsuarioId(usuario.getId()).stream()
                .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                .map(MiembroEquipo::getEquipo)
                .map(EquipoBusquedaResponse::fromEntity)
                .toList();
    }

    // ---------- helpers ----------

    private java.util.Optional<MiembroEquipo> capitanDe(Long equipoId) {
        return miembroEquipoRepository.findByEquipoId(equipoId).stream()
                .filter(m -> "CAPITAN".equals(m.getRol()) && ESTADO_ACTIVO.equals(m.getEstado()))
                .findFirst();
    }

    private void exigirCapitan(Long equipoId, Usuario usuario) {
        boolean esCapitan = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, usuario.getId())
                .filter(m -> "CAPITAN".equals(m.getRol()) && ESTADO_ACTIVO.equals(m.getEstado()))
                .isPresent();
        if (!esCapitan) {
            throw new ForbiddenException("Solo el capitán del equipo puede gestionar las solicitudes");
        }
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }

    private static String normalizar(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
