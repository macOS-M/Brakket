package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.notification.service.NotificationService;
import com.coffeecommits.brakket.team.dto.InvitacionResponse;
import com.coffeecommits.brakket.team.dto.InvitarJugadorRequest;
import com.coffeecommits.brakket.team.dto.ResponderInvitacionRequest;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.InvitacionEquipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.InvitacionEquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TeamInvitationServiceImpl implements TeamInvitationService {

    private static final Set<String> ROLES_VALIDOS = Set.of("CAPITAN", "TITULAR", "SUPLENTE", "COACH");
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final int MAX_MIEMBROS_ACTIVOS = 10;

    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final InvitacionEquipoRepository invitacionRepository;
    private final NotificationService notificationService;

    public TeamInvitationServiceImpl(EquipoRepository equipoRepository,
                                     UsuarioRepository usuarioRepository,
                                     MiembroEquipoRepository miembroEquipoRepository,
                                     InvitacionEquipoRepository invitacionRepository,
                                     NotificationService notificationService) {
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.invitacionRepository = invitacionRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public InvitacionResponse invitar(Long equipoId, InvitarJugadorRequest request, String capitanCorreo) {

        Usuario capitan = usuarioRepository.findByCorreo(capitanCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", capitanCorreo));

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        MiembroEquipo miembroCapitan = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, capitan.getId())
                .orElseThrow(() -> new BusinessException("No tiene permisos sobre este equipo"));

        if (!"CAPITAN".equals(miembroCapitan.getRol())) {
            throw new BusinessException("Solo el capitan del equipo puede invitar jugadores");
        }

        String rolPropuesto = request.rolPropuesto().toUpperCase();
        if (!ROLES_VALIDOS.contains(rolPropuesto)) {
            throw new BusinessException(
                    "Rol invalido '%s'. Los roles permitidos son: %s".formatted(rolPropuesto, ROLES_VALIDOS));
        }

        Usuario jugador = usuarioRepository.findById(request.jugadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.jugadorId()));

        miembroEquipoRepository.findByEquipoIdAndUsuarioId(equipoId, jugador.getId())
                .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                .ifPresent(m -> {
                    throw new BusinessException("El jugador ya pertenece al equipo");
                });

        long activos = miembroEquipoRepository.findByEquipoId(equipoId).stream()
                .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                .count();
        if (activos >= MAX_MIEMBROS_ACTIVOS) {
            throw new BusinessException("El equipo no tiene cupo disponible");
        }

        if (invitacionRepository.existsByEquipoIdAndJugadorIdAndEstado(equipoId, jugador.getId(), ESTADO_PENDIENTE)) {
            throw new BusinessException("Ya existe una invitacion pendiente para este jugador");
        }

        InvitacionEquipo invitacion = InvitacionEquipo.builder()
                .equipo(equipo)
                .jugador(jugador)
                .rolPropuesto(rolPropuesto)
                .mensaje(request.mensaje())
                .estado(ESTADO_PENDIENTE)
                .creadoPor(capitan)
                .fechaCreacion(LocalDateTime.now())
                .build();
        invitacion = invitacionRepository.save(invitacion);

        notificationService.notificar(
                jugador,
                "INVITACION_EQUIPO",
                "%s te invito a unirte al equipo '%s' como %s".formatted(
                        capitan.getNombre(), equipo.getNombre(), rolPropuesto),
                "InvitacionEquipo",
                invitacion.getId());

        return InvitacionResponse.fromEntity(invitacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitacionResponse> misInvitacionesPendientes(String jugadorCorreo) {
        Usuario jugador = usuarioRepository.findByCorreo(jugadorCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", jugadorCorreo));

        return invitacionRepository.findByJugadorIdAndEstado(jugador.getId(), ESTADO_PENDIENTE).stream()
                .map(InvitacionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public InvitacionResponse responder(Long invitacionId, ResponderInvitacionRequest request, String jugadorCorreo) {

        Usuario jugador = usuarioRepository.findByCorreo(jugadorCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", jugadorCorreo));

        InvitacionEquipo invitacion = invitacionRepository.findByIdAndJugadorId(invitacionId, jugador.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitacion", invitacionId));

        if (!ESTADO_PENDIENTE.equals(invitacion.getEstado())) {
            throw new BusinessException("La invitacion ya no esta pendiente");
        }

        if (Boolean.TRUE.equals(request.aceptar())) {
            long activos = miembroEquipoRepository.findByEquipoId(invitacion.getEquipo().getId()).stream()
                    .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                    .count();
            if (activos >= MAX_MIEMBROS_ACTIVOS) {
                throw new BusinessException("El equipo ya no tiene cupo disponible");
            }

            MiembroEquipo miembro = MiembroEquipo.builder()
                    .equipo(invitacion.getEquipo())
                    .usuario(jugador)
                    .estado(ESTADO_ACTIVO)
                    .fechaUnion(LocalDate.now())
                    .rol(invitacion.getRolPropuesto())
                    .build();
            try {
                miembroEquipoRepository.save(miembro);
            } catch (DataIntegrityViolationException e) {
                throw new BusinessException("El jugador ya pertenece al equipo");
            }

            invitacion.setEstado("ACEPTADA");
            notificationService.notificar(
                    invitacion.getCreadoPor(),
                    "INVITACION_ACEPTADA",
                    "%s acepto tu invitacion al equipo '%s'".formatted(
                            jugador.getNombre(), invitacion.getEquipo().getNombre()),
                    "InvitacionEquipo",
                    invitacion.getId());
        } else {
            invitacion.setEstado("RECHAZADA");
            notificationService.notificar(
                    invitacion.getCreadoPor(),
                    "INVITACION_RECHAZADA",
                    "%s rechazo tu invitacion al equipo '%s'".formatted(
                            jugador.getNombre(), invitacion.getEquipo().getNombre()),
                    "InvitacionEquipo",
                    invitacion.getId());
        }

        invitacion.setFechaRespuesta(LocalDateTime.now());
        invitacionRepository.save(invitacion);

        return InvitacionResponse.fromEntity(invitacion);
    }
}