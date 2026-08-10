package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.admin.model.LogAuditoria;
import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeamRegistrationServiceImpl implements TeamRegistrationService {

    /** Estados de inscripción que ya no comprometen al equipo (convención de RF-03). */
    private static final Set<String> INSCRIPCION_ESTADOS_CERRADOS =
            Set.of("RECHAZADA", "CANCELADA", "FINALIZADA");

    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final JuegoRepository juegoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final EquipoRedSocialRepository redSocialRepository;
    private final InscripcionRepository inscripcionRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;

    public TeamRegistrationServiceImpl(EquipoRepository equipoRepository,
                                       UsuarioRepository usuarioRepository,
                                       UsuarioRolRepository usuarioRolRepository,
                                       JuegoRepository juegoRepository,
                                       MiembroEquipoRepository miembroEquipoRepository,
                                       EquipoRedSocialRepository redSocialRepository,
                                       InscripcionRepository inscripcionRepository,
                                       LogAuditoriaRepository logAuditoriaRepository) {
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.juegoRepository = juegoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.redSocialRepository = redSocialRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    @Override
    @Transactional
    public EquipoResponse crear(CrearEquipoRequest request, String creadorCorreo, boolean esAdmin) {

        Usuario creador = usuarioRepository.findByCorreo(creadorCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", creadorCorreo));

        // Los administradores moderan, no juegan: el equipo lo crean PARA
        // otros jugadores, designando por correo a su capitán. Un jugador
        // normal queda él mismo como capitán y miembro fundador.
        Usuario capitan = esAdmin ? capitanDesignado(request, creador) : creador;

        equipoRepository.findByNombre(request.nombre()).ifPresent(e -> {
            throw new BusinessException(
                    "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
        });

        List<Juego> juegos = buscarJuegosActivos(request.juegoIds(), request.juegoId());
        Juego juego = juegoPrincipal(juegos, request.juegoId());

        Equipo equipoNuevo = Equipo.builder()
                .nombre(request.nombre())
                .logo(request.logo())
                .bannerUrl(request.bannerUrl())
                .descripcion(request.descripcion())
                .sitioWeb(request.sitioWeb())
                .videoUrl(request.videoUrl())
                .capitan(capitan)
                .juego(juego)
                .juegos(new LinkedHashSet<>(juegos))
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
                .usuario(capitan)
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
    public EquipoResponse editar(Long equipoId, EditarEquipoRequest request,
                                 String actorCorreo, boolean esAdmin) {

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        Usuario actor = usuarioRepository.findByCorreo(actorCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", actorCorreo));

        // La capitanía vigente vive en miembro_equipo (RF-09 permite transferirla
        // y tener más de un capitán); equipo.capitan solo registra al fundador.
        // El ADMIN edita cualquier equipo como moderación, sea o no miembro.
        boolean esCapitanActivo = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, actor.getId())
                .filter(m -> "CAPITAN".equals(m.getRol()) && "ACTIVO".equals(m.getEstado()))
                .isPresent();
        if (!esCapitanActivo && !esAdmin) {
            throw new AccessDeniedException(
                    "Solo el capitán del equipo (o un administrador) puede editar su información.");
        }

        if ("DISUELTO".equals(equipo.getEstado())) {
            throw new BusinessException("El equipo fue disuelto; no se puede editar su información.");
        }
        if ("BLOQUEADO".equals(equipo.getEstado())) {
            throw new BusinessException(
                    "El equipo está bloqueado por una revisión administrativa o disputa activa.");
        }

        // Concurrencia optimista (RF-02): el cliente manda la versión que leyó
        // en el GET; si otro usuario guardó entre medio, se rechaza con 409 en
        // vez de pisar sus cambios en silencio.
        if (request.version() != null && !request.version().equals(equipo.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Equipo.class, equipoId);
        }

        if (request.nombre() != null && !request.nombre().isBlank()
                && !request.nombre().equals(equipo.getNombre())) {
            equipoRepository.findByNombreIgnoreCase(request.nombre()).ifPresent(existente -> {
                if (!existente.getId().equals(equipoId)) {
                    throw new BusinessException(
                            "Ya existe un equipo con el nombre '%s'".formatted(request.nombre()));
                }
            });
            equipo.setNombre(request.nombre());
        }

        // Contrato de edición parcial: null = no tocar; string vacío = borrar
        // (es la única forma que tiene el formulario de limpiar un campo).
        if (request.logo() != null) {
            equipo.setLogo(request.logo().isBlank() ? null : request.logo());
        }

        if (request.bannerUrl() != null) {
            equipo.setBannerUrl(request.bannerUrl().isBlank() ? null : request.bannerUrl());
        }

        if (request.descripcion() != null) {
            equipo.setDescripcion(request.descripcion().isBlank() ? null : request.descripcion());
        }

        if (request.sitioWeb() != null) {
            equipo.setSitioWeb(request.sitioWeb().isBlank() ? null : request.sitioWeb());
        }

        if (request.videoUrl() != null) {
            equipo.setVideoUrl(request.videoUrl().isBlank() ? null : request.videoUrl());
        }

        if (request.estadoPrivacidad() != null) {
            equipo.setEstadoPrivacidad(request.estadoPrivacidad());
        }

        if (request.juegoIds() != null || request.juegoId() != null) {
            List<Juego> nuevosJuegos = buscarJuegosActivos(request.juegoIds(), request.juegoId());
            Set<Long> juegosActuales = equipo.getJuegos().isEmpty()
                    ? Set.of(equipo.getJuego().getId())
                    : equipo.getJuegos().stream().map(Juego::getId).collect(java.util.stream.Collectors.toSet());
            Set<Long> juegosSolicitados = nuevosJuegos.stream().map(Juego::getId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!juegosActuales.equals(juegosSolicitados) && participaEnTorneoActivo(equipoId)) {
                throw new BusinessException(
                        "No se pueden cambiar los juegos mientras el equipo participa en un torneo activo.");
            }
            equipo.setJuego(juegoPrincipal(nuevosJuegos, request.juegoId()));
            equipo.getJuegos().clear();
            equipo.getJuegos().addAll(nuevosJuegos);
        }

        if (request.redesSociales() != null) {
            List<EquipoRedSocial> existentes = redSocialRepository.findByEquipoId(equipoId);
            redSocialRepository.deleteAll(existentes);
            request.redesSociales().forEach(url ->
                    redSocialRepository.save(EquipoRedSocial.builder().equipo(equipo).url(url).build()));
        }

        // La entidad es managed: el UPDATE se difiere al commit, así que un
        // try/catch de integridad aquí nunca atraparía nada. La carrera de
        // nombre duplicado la cubre el UNIQUE de la BD.
        Equipo equipoActualizado = equipoRepository.save(equipo);

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

    private List<Juego> buscarJuegosActivos(List<Long> juegoIds, Long juegoIdCompatibilidad) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (juegoIdCompatibilidad != null) ids.add(juegoIdCompatibilidad);
        if (juegoIds != null) ids.addAll(juegoIds);
        if (ids.isEmpty()) throw new BusinessException("Debés seleccionar al menos un juego.");

        List<Juego> encontrados = ids.stream()
                .map(id -> juegoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Juego", id)))
                .toList();
        if (encontrados.stream().anyMatch(j -> !Boolean.TRUE.equals(j.getActivo()))) {
            throw new BusinessException("Todos los juegos seleccionados deben estar activos.");
        }
        return ids.stream().map(id -> encontrados.stream()
                .filter(j -> j.getId().equals(id)).findFirst().orElseThrow()).toList();
    }

    private Juego juegoPrincipal(List<Juego> juegos, Long juegoIdPrincipal) {
        if (juegoIdPrincipal == null) return juegos.get(0);
        return juegos.stream().filter(j -> j.getId().equals(juegoIdPrincipal)).findFirst()
                .orElseThrow(() -> new BusinessException(
                        "El juego principal debe formar parte de los juegos del equipo."));
    }

    /**
     * "Activo" = torneo vigente o futuro (fechaFin >= hoy) con una inscripción
     * que no esté cerrada. Un torneo que empieza mañana también bloquea el
     * cambio de disciplina: el equipo ya está comprometido con ese juego.
     * Los estados cerrados siguen la misma convención que RF-03; revisar
     * cuando el módulo tournament formalice sus estados.
     */
    /**
     * El jugador que capitaneará un equipo creado por un ADMIN: debe venir
     * indicado por correo, existir, no estar bloqueado y no ser otro
     * administrador (los ADMIN no forman parte de equipos).
     */
    private Usuario capitanDesignado(CrearEquipoRequest request, Usuario creador) {
        String correo = request.capitanCorreo() == null ? "" : request.capitanCorreo().trim();
        if (correo.isEmpty()) {
            throw new BusinessException(
                    "Un administrador crea el equipo para otros jugadores: indicá el correo del capitán designado.");
        }
        Usuario capitan = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new BusinessException(
                        "No existe un usuario con el correo '%s' para designar como capitán.".formatted(correo)));
        if (capitan.getId().equals(creador.getId())
                || usuarioRolRepository.existsByUsuarioIdAndRolNombreRol(capitan.getId(), "ADMIN")) {
            throw new BusinessException(
                    "Un administrador no puede formar parte de un equipo: designá a un jugador como capitán.");
        }
        if (Boolean.TRUE.equals(capitan.getBloqueado())) {
            throw new BusinessException(
                    "El usuario designado como capitán está bloqueado en la plataforma.");
        }
        // Un roster a la vez: sin este freno, el admin generaría doble
        // membresía activa y rompería el supuesto de invitaciones y pases.
        if (miembroEquipoRepository.existsByUsuarioIdAndEstado(capitan.getId(), "ACTIVO")) {
            throw new BusinessException(
                    "'%s' ya es miembro activo de otro equipo: correspondería una transferencia."
                            .formatted(capitan.getNombre()));
        }
        return capitan;
    }

    private boolean participaEnTorneoActivo(Long equipoId) {
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        List<Inscripcion> inscripciones = inscripcionRepository.findByEquipoId(equipoId);
        return inscripciones.stream()
                .filter(inscripcion -> inscripcion.getEstado() == null
                        || !INSCRIPCION_ESTADOS_CERRADOS.contains(inscripcion.getEstado().toUpperCase()))
                .map(Inscripcion::getTorneo)
                // Sin fecha de fin (torneo del modelo abierto) se considera
                // activo mientras su inscripción siga vigente.
                .anyMatch(torneo -> torneo.getFechaFin() == null || !ahora.isAfter(torneo.getFechaFin()));
    }
}
