package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.game.repository.PerfilCompetitivoRepository;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.dto.CrearTorneoRequest;
import com.coffeecommits.brakket.tournament.dto.EquipoElegibleResponse;
import com.coffeecommits.brakket.tournament.dto.EquipoInscritoResponse;
import com.coffeecommits.brakket.tournament.dto.TorneoDetalleResponse;
import com.coffeecommits.brakket.tournament.dto.TorneoResponse;
import com.coffeecommits.brakket.tournament.model.AjustePartida;
import com.coffeecommits.brakket.tournament.model.FormatoTorneo;
import com.coffeecommits.brakket.tournament.model.Inscripcion;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TorneoServiceImpl implements TorneoService {

    /** Estados de inscripción que no ocupan cupo. */
    private static final List<String> INSCRIPCION_CERRADA = List.of("RECHAZADA", "CANCELADA");

    private final TorneoRepository torneoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final JuegoRepository juegoRepository;
    private final TemporadaRepository temporadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilCompetitivoRepository perfilCompetitivoRepository;

    public TorneoServiceImpl(TorneoRepository torneoRepository,
                             InscripcionRepository inscripcionRepository,
                             JuegoRepository juegoRepository,
                             TemporadaRepository temporadaRepository,
                             UsuarioRepository usuarioRepository,
                             PerfilCompetitivoRepository perfilCompetitivoRepository) {
        this.torneoRepository = torneoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.juegoRepository = juegoRepository;
        this.temporadaRepository = temporadaRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilCompetitivoRepository = perfilCompetitivoRepository;
    }

    @Override
    @Transactional
    public TorneoResponse crearTorneo(String correo, boolean esAdmin, CrearTorneoRequest request) {
        Usuario organizador = buscarUsuario(correo);

        Juego juego = juegoRepository.findById(request.juegoId())
                .orElseThrow(() -> new ResourceNotFoundException("Juego", request.juegoId()));
        if (!Boolean.TRUE.equals(juego.getActivo())) {
            throw new BusinessException(
                    "El juego '%s' no está disponible para torneos".formatted(juego.getNombre()));
        }

        // Margen de 5 minutos: tolera desfases de reloj entre el navegador
        // y el servidor sin rechazar torneos que arrancan "ya mismo".
        if (request.fechaInicio().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BusinessException(
                    "La fecha de inicio ya pasó (hora del servidor: %s)"
                            .formatted(LocalDateTime.now().withNano(0)));
        }

        // El perfil competitivo (RF-21) actúa como curaduría opcional: si
        // existe, acota el tamaño de equipo; si no, valen los defaults.
        perfilCompetitivoRepository.findByJuegoId(juego.getId())
                .filter(perfil -> Boolean.TRUE.equals(perfil.getActivo()))
                .ifPresent(perfil -> {
                    if (request.tamanoEquipo() < perfil.getPlantillaMinima()
                            || request.tamanoEquipo() > perfil.getPlantillaMaxima()) {
                        throw new BusinessException(
                                "El perfil competitivo de %s exige equipos de %d a %d jugadores"
                                        .formatted(juego.getNombre(),
                                                perfil.getPlantillaMinima(),
                                                perfil.getPlantillaMaxima()));
                    }
                });

        Temporada temporada = null;
        if (request.temporadaId() != null) {
            temporada = temporadaRepository.findById(request.temporadaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Temporada", request.temporadaId()));
            if (!temporada.getLiga().getJuego().getId().equals(juego.getId())) {
                throw new BusinessException(
                        "La temporada pertenece a una liga de otro juego");
            }
            boolean esDuenoLiga = temporada.getLiga().getComisionado().getId()
                    .equals(organizador.getId());
            if (!esDuenoLiga && !esAdmin) {
                throw new ForbiddenException(
                        "Solo el comisionado de la liga puede hospedar torneos en su temporada");
            }
        }

        Torneo torneo = torneoRepository.save(Torneo.builder()
                .juego(juego)
                .temporada(temporada)
                .organizador(organizador)
                .nombre(request.nombre().trim())
                .descripcion(normalizar(request.descripcion()))
                .formato(formatoValido(request.formato()))
                .tamanoEquipo(request.tamanoEquipo())
                .maxEquipos(request.maxEquipos())
                .fechaInicio(request.fechaInicio())
                .estado(EstadoTorneo.INSCRIPCION_ABIERTA)
                .publico(request.publico() == null || request.publico())
                .premio(normalizar(request.premio()))
                .ajustesPartida(normalizarAjustes(request.ajustesPartida()))
                .build());
        return TorneoResponse.from(torneo, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TorneoResponse> listar(Long juegoId, String correoOpcional) {
        List<Torneo> publicos = juegoId == null
                ? torneoRepository.findByPublicoTrueOrderByFechaInicioAsc()
                : torneoRepository.findByJuegoIdAndPublicoTrueOrderByFechaInicioAsc(juegoId);

        // Con sesión se suman los torneos propios (incluidos los privados).
        Map<Long, Torneo> visibles = new LinkedHashMap<>();
        publicos.forEach(t -> visibles.put(t.getId(), t));
        if (correoOpcional != null) {
            usuarioRepository.findByCorreo(correoOpcional).ifPresent(usuario ->
                    torneoRepository.findByOrganizadorIdOrderByFechaInicioAsc(usuario.getId()).stream()
                            .filter(t -> juegoId == null || t.getJuego().getId().equals(juegoId))
                            .forEach(t -> visibles.put(t.getId(), t)));
        }

        return visibles.values().stream()
                .sorted((a, b) -> a.getFechaInicio().compareTo(b.getFechaInicio()))
                .map(t -> TorneoResponse.from(t, inscripcionRepository.countVigentesPorTorneo(t.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TorneoResponse> misCompetencias(String correo) {
        Usuario usuario = buscarUsuario(correo);
        Map<Long, Torneo> visibles = new LinkedHashMap<>();
        torneoRepository.findByOrganizadorIdOrderByFechaInicioAsc(usuario.getId())
                .forEach(t -> visibles.put(t.getId(), t));
        inscripcionRepository.inscripcionesVigentesDeUsuario(usuario.getId())
                .forEach(i -> visibles.put(i.getTorneo().getId(), i.getTorneo()));
        return visibles.values().stream()
                .sorted((a, b) -> a.getFechaInicio().compareTo(b.getFechaInicio()))
                .map(t -> TorneoResponse.from(t, inscripcionRepository.countVigentesPorTorneo(t.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TorneoDetalleResponse obtenerDetalle(Long torneoId, String correoOpcional, boolean esAdmin) {
        Torneo torneo = buscarVisible(torneoId, correoOpcional, esAdmin);
        return detalleDe(torneo);
    }

    @Override
    @Transactional
    public TorneoDetalleResponse inscribirEquipo(Long torneoId, String correo, Long equipoId,
                                                 String usuarioEnJuego) {
        Usuario usuario = buscarUsuario(correo);
        Torneo torneo = buscarVisible(torneoId, correo, false);

        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_ABIERTA) {
            throw new BusinessException("El torneo ya no acepta inscripciones");
        }
        if (!torneo.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new BusinessException("El torneo ya comenzó: no acepta inscripciones");
        }
        long vigentes = inscripcionRepository.countVigentesPorTorneo(torneoId);
        if (vigentes >= torneo.getMaxEquipos()) {
            throw new BusinessException("El torneo ya alcanzó su cupo de equipos");
        }

        if (!inscripcionRepository.esCapitanActivo(usuario.getId(), equipoId)) {
            throw new ForbiddenException("Solo el capitán del equipo puede inscribirlo");
        }
        if (inscripcionRepository.existsByTorneoIdAndEquipoId(torneoId, equipoId)) {
            throw new BusinessException("El equipo ya está inscrito en este torneo");
        }

        Equipo equipo = inscripcionRepository.equiposCapitaneadosPor(usuario.getId()).stream()
                .filter(e -> e.getId().equals(equipoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        // El juego del equipo es su disciplina favorita, no una restricción:
        // cualquier equipo puede competir en torneos de otros juegos.
        long plantilla = inscripcionRepository.countMiembrosActivos(equipoId);
        if (plantilla < torneo.getTamanoEquipo()) {
            throw new BusinessException(
                    "El torneo es %dv%d y el equipo tiene %d jugador(es) activo(s)"
                            .formatted(torneo.getTamanoEquipo(), torneo.getTamanoEquipo(), plantilla));
        }

        inscripcionRepository.save(Inscripcion.builder()
                .torneo(torneo)
                .equipo(equipo)
                .estado("CONFIRMADA")
                .fechaSolicitud(LocalDate.now())
                .usuarioEnJuego(usuarioEnJuego.trim())
                .build());
        return detalleDe(torneo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoElegibleResponse> equiposElegibles(Long torneoId, String correo) {
        Usuario usuario = buscarUsuario(correo);
        buscarVisible(torneoId, correo, false); // 404 si es privado y ajeno


        // Sin filtro por juego: la disciplina del equipo es preferencia,
        // no requisito (cualquier equipo compite donde quiera).
        return inscripcionRepository.equiposCapitaneadosPor(usuario.getId()).stream()
                .filter(equipo -> !inscripcionRepository
                        .existsByTorneoIdAndEquipoId(torneoId, equipo.getId()))
                .map(EquipoElegibleResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void eliminarTorneo(Long torneoId, String correo, boolean esAdmin) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo", torneoId));
        if (!esAdmin) {
            Usuario usuario = buscarUsuario(correo);
            if (!torneo.getOrganizador().getId().equals(usuario.getId())) {
                throw new ForbiddenException(
                        "Solo el organizador o un administrador pueden eliminar este torneo");
            }
        }
        // Un torneo con la llave en juego no se borra por accidente (se
        // llevaría bracket y resultados por el ON DELETE CASCADE).
        if (torneo.getEstado() == EstadoTorneo.EN_CURSO) {
            throw new BusinessException(
                    "El torneo está en curso: terminalo o resolvé sus partidas antes de eliminarlo");
        }
        // Las inscripciones caen por el ON DELETE CASCADE del esquema.
        torneoRepository.delete(torneo);
    }

    // ---------- helpers ----------

    /** Un torneo privado solo existe para su organizador o un ADMIN. */
    private Torneo buscarVisible(Long torneoId, String correoOpcional, boolean esAdmin) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo", torneoId));
        if (Boolean.TRUE.equals(torneo.getPublico()) || esAdmin) {
            return torneo;
        }
        Long organizadorId = torneo.getOrganizador().getId();
        boolean esOrganizador = correoOpcional != null && usuarioRepository.findByCorreo(correoOpcional)
                .map(u -> Objects.equals(u.getId(), organizadorId))
                .orElse(false);
        if (!esOrganizador) {
            throw new ResourceNotFoundException("Torneo", torneoId);
        }
        return torneo;
    }

    private TorneoDetalleResponse detalleDe(Torneo torneo) {
        List<EquipoInscritoResponse> equipos = inscripcionRepository.findByTorneoId(torneo.getId()).stream()
                .filter(i -> !INSCRIPCION_CERRADA.contains(i.getEstado()))
                .map(i -> new EquipoInscritoResponse(
                        i.getEquipo().getId(),
                        i.getEquipo().getNombre(),
                        i.getEquipo().getLogo(),
                        i.getUsuarioEnJuego(),
                        inscripcionRepository.miembrosActivosDeEquipo(i.getEquipo().getId()).stream()
                                .map(m -> new EquipoInscritoResponse.JugadorInscritoResponse(
                                        m.getUsuario().getId(),
                                        m.getUsuario().getNombre(),
                                        m.getRol()))
                                .toList()))
                .toList();
        return new TorneoDetalleResponse(TorneoResponse.from(torneo, equipos.size()), equipos);
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }

    private static String normalizar(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    /**
     * El formato se guarda con su código canónico del catálogo: es lo que
     * el motor de llaves interpreta al iniciar el torneo (DD-05 cerrada).
     */
    private static String formatoValido(String formato) {
        return FormatoTorneo.interpretar(formato)
                .orElseThrow(() -> new BusinessException(
                        "Formato no soportado: %s. Valen eliminación directa, doble eliminación, round robin, suizo o fase de grupos y eliminación"
                                .formatted(formato)))
                .name();
    }

    /** Descarta pares vacíos y recorta espacios de los ajustes de partida. */
    private static List<AjustePartida> normalizarAjustes(List<AjustePartida> ajustes) {
        if (ajustes == null) {
            return List.of();
        }
        return ajustes.stream()
                .filter(a -> a != null && a.clave() != null && !a.clave().isBlank()
                        && a.valor() != null && !a.valor().isBlank())
                .map(a -> new AjustePartida(a.clave().trim(), a.valor().trim()))
                .toList();
    }
}
