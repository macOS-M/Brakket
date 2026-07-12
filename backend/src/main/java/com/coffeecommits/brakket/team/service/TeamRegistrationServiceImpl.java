package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.admin.model.LogAuditoria;
import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.team.dto.CrearEquipoRequest;
import com.coffeecommits.brakket.team.dto.EditarEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.EquipoRedSocial;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRedSocialRepository;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.model.Inscripcion;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeamRegistrationServiceImpl implements TeamRegistrationService {

    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final JuegoRepository juegoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final EquipoRedSocialRepository redSocialRepository;
    private final InscripcionRepository inscripcionRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;

    public TeamRegistrationServiceImpl(EquipoRepository equipoRepository,
                                       UsuarioRepository usuarioRepository,
                                       JuegoRepository juegoRepository,
                                       MiembroEquipoRepository miembroEquipoRepository,
                                       EquipoRedSocialRepository redSocialRepository,
                                       InscripcionRepository inscripcionRepository,
                                       LogAuditoriaRepository logAuditoriaRepository) {
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.juegoRepository = juegoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.redSocialRepository = redSocialRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    @Override
    @Transactional
    public EquipoResponse crear(CrearEquipoRequest request, String creadorCorreo) {

        Usuario creador = usuarioRepository.findByCorreo(creadorCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", creadorCorreo));

        equipoRepository.findByNombre(request.nombre()).ifPresent(e -> {
            throw new BusinessException(
                    "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
        });

        Juego juego = juegoRepository.findById(request.juegoId())
                .orElseThrow(() -> new ResourceNotFoundException("Juego", request.juegoId()));

        if (!Boolean.TRUE.equals(juego.getActivo())) {
            throw new BusinessException("El juego seleccionado no esta activo");
        }

        Equipo equipoNuevo = Equipo.builder()
                .nombre(request.nombre())
                .logo(request.logo())
                .descripcion(request.descripcion())
                .capitan(creador)
                .juego(juego)
                .build();

        final Equipo equipoGuardado;
        try {
            equipoGuardado = equipoRepository.save(equipoNuevo);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
        }

        MiembroEquipo miembro = MiembroEquipo.builder()
                .equipo(equipoGuardado)
                .usuario(creador)
                .estado("ACTIVO")
                .fechaUnion(LocalDate.now())
                .rol("CAPITAN")
                .build();
        miembroEquipoRepository.save(miembro);

        List<String> redes = request.redesSociales() == null ? List.of() : request.redesSociales();
        List<String> redesGuardadas = redes.stream()
                .map(url -> redSocialRepository.save(
                        EquipoRedSocial.builder().equipo(equipoGuardado).url(url).build()))
                .map(EquipoRedSocial::getUrl)
                .toList();

        return EquipoResponse.fromEntity(equipoGuardado, redesGuardadas);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoResponse obtenerPorId(Long equipoId) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        List<String> redes = redSocialRepository.findByEquipoId(equipoId).stream()
                .map(EquipoRedSocial::getUrl)
                .toList();

        return EquipoResponse.fromEntity(equipo, redes);
    }

    /**
     * RF-02: edición parcial de equipo por su capitán.
     * Orden de validación: existencia -> identidad (capitán) -> estado del
     * equipo (bloqueado/disuelto) -> validación de cada campo enviado.
     * Toda la operación corre en una única transacción, lo que además cubre
     * el criterio "si el capitán pierde permisos durante la operación, el
     * sistema cancela el guardado": no hay ventana intermedia donde otra
     * transacción pueda alterar la capitanía a mitad de camino.
     */
    @Override
    @Transactional
    public EquipoResponse editar(Long equipoId, EditarEquipoRequest request, String actorCorreo) {

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        Usuario actor = usuarioRepository.findByCorreo(actorCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", actorCorreo));

        if (!equipo.getCapitan().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Solo el capitán del equipo puede editar su información.");
        }

        if ("DISUELTO".equals(equipo.getEstado())) {
            throw new BusinessException("El equipo fue disuelto; no se puede editar su información.");
        }
        if ("BLOQUEADO".equals(equipo.getEstado())) {
            throw new BusinessException(
                    "El equipo está bloqueado por una revisión administrativa o disputa activa.");
        }

        if (request.nombre() != null && !request.nombre().isBlank()
                && !request.nombre().equals(equipo.getNombre())) {
            equipoRepository.findByNombre(request.nombre()).ifPresent(existente -> {
                if (!existente.getId().equals(equipoId)) {
                    throw new BusinessException(
                            "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
                }
            });
            equipo.setNombre(request.nombre());
        }

        if (request.logo() != null) {
            equipo.setLogo(request.logo());
        }

        if (request.descripcion() != null) {
            equipo.setDescripcion(request.descripcion());
        }

        if (request.estadoPrivacidad() != null) {
            equipo.setEstadoPrivacidad(request.estadoPrivacidad());
        }

        if (request.juegoId() != null
                && (equipo.getJuego() == null || !request.juegoId().equals(equipo.getJuego().getId()))) {

            Juego nuevoJuego = juegoRepository.findById(request.juegoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Juego", request.juegoId()));

            if (!Boolean.TRUE.equals(nuevoJuego.getActivo())) {
                throw new BusinessException("El juego seleccionado no está activo.");
            }

            if (participaEnTorneoActivo(equipoId)) {
                throw new BusinessException(
                        "No se puede cambiar la disciplina: el equipo participa en un torneo activo.");
            }

            equipo.setJuego(nuevoJuego);
        }

        if (request.redesSociales() != null) {
            List<EquipoRedSocial> existentes = redSocialRepository.findByEquipoId(equipoId);
            redSocialRepository.deleteAll(existentes);
            request.redesSociales().forEach(url ->
                    redSocialRepository.save(EquipoRedSocial.builder().equipo(equipo).url(url).build()));
        }

        final Equipo equipoActualizado;
        try {
            equipoActualizado = equipoRepository.save(equipo);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
        }

        logAuditoriaRepository.save(LogAuditoria.builder()
                .usuario(actor)
                .accion("EQUIPO_EDITADO")
                .entidad("equipo")
                .entidadId(equipoId)
                .fecha(LocalDateTime.now())
                .build());

        List<String> redesActuales = redSocialRepository.findByEquipoId(equipoId).stream()
                .map(EquipoRedSocial::getUrl)
                .toList();

        return EquipoResponse.fromEntity(equipoActualizado, redesActuales);
    }

    /**
     * "Activo" se determina por rango de fechas del torneo (fechaInicio <=
     * hoy <= fechaFin), ya que el módulo tournament aún no define valores
     * canónicos para Torneo.estado. Revisar esta lógica cuando ese módulo
     * tenga su propio servicio con estados formalizados.
     */
    private boolean participaEnTorneoActivo(Long equipoId) {
        LocalDate hoy = LocalDate.now();
        List<Inscripcion> inscripciones = inscripcionRepository.findByEquipoId(equipoId);
        return inscripciones.stream()
                .map(Inscripcion::getTorneo)
                .anyMatch(torneo -> !hoy.isBefore(torneo.getFechaInicio())
                        && !hoy.isAfter(torneo.getFechaFin()));
    }
}