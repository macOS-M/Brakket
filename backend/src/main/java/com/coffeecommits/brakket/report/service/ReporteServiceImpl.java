package com.coffeecommits.brakket.report.service;

import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.service.AuthService;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.report.dto.FiltrosReporteRequest;
import com.coffeecommits.brakket.report.dto.ReporteResponse;
import com.coffeecommits.brakket.report.model.ReporteGenerado;
import com.coffeecommits.brakket.report.model.TipoReporte;
import com.coffeecommits.brakket.report.repository.ReporteGeneradoRepository;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.sponsorship.service.PatrocinioService;
import com.coffeecommits.brakket.statistics.repository.EstadisticaJugadorRepository;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class ReporteServiceImpl implements ReporteService {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final PatrocinadorRepository patrocinadorRepository;
    private final PatrocinioRepository patrocinioRepository;
    private final PatrocinioService patrocinioService;
    private final TorneoRepository torneoRepository;
    private final PartidaRepository partidaRepository;
    private final DisputaRepository disputaRepository;
    private final TransmisionTwitchRepository transmisionRepository;
    private final MetricaAudienciaRepository metricaAudienciaRepository;
    private final MetricaChatRepository metricaChatRepository;
    private final AnalisisSentimientoRepository sentimientoRepository;
    private final EstadisticaJugadorRepository estadisticaJugadorRepository;
    private final ReporteGeneradoRepository reporteGeneradoRepository;

    public ReporteServiceImpl(AuthService authService, UsuarioRepository usuarioRepository,
                              PatrocinadorRepository patrocinadorRepository, PatrocinioRepository patrocinioRepository,
                              PatrocinioService patrocinioService, TorneoRepository torneoRepository,
                              PartidaRepository partidaRepository, DisputaRepository disputaRepository,
                              TransmisionTwitchRepository transmisionRepository,
                              MetricaAudienciaRepository metricaAudienciaRepository, MetricaChatRepository metricaChatRepository,
                              AnalisisSentimientoRepository sentimientoRepository,
                              EstadisticaJugadorRepository estadisticaJugadorRepository,
                              ReporteGeneradoRepository reporteGeneradoRepository) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
        this.patrocinadorRepository = patrocinadorRepository;
        this.patrocinioRepository = patrocinioRepository;
        this.patrocinioService = patrocinioService;
        this.torneoRepository = torneoRepository;
        this.partidaRepository = partidaRepository;
        this.disputaRepository = disputaRepository;
        this.transmisionRepository = transmisionRepository;
        this.metricaAudienciaRepository = metricaAudienciaRepository;
        this.metricaChatRepository = metricaChatRepository;
        this.sentimientoRepository = sentimientoRepository;
        this.estadisticaJugadorRepository = estadisticaJugadorRepository;
        this.reporteGeneradoRepository = reporteGeneradoRepository;
    }

    @Override
    public ReporteResponse generar(TipoReporte tipo, FiltrosReporteRequest filtros, Authentication authentication) {
        Long patrocinadorId = resolverPatrocinadorFiltro(authentication, filtros);
        FiltrosReporteRequest efectivos =
                new FiltrosReporteRequest(filtros.torneoId(), patrocinadorId, filtros.desde(), filtros.hasta());

        DatosReporte datos = switch (tipo) {
            case PATROCINIO -> generarPatrocinio(efectivos);
            case AUDIENCIA -> generarAudiencia(efectivos);
            case COMPETENCIA -> generarCompetencia(efectivos);
            case ESTADISTICA -> generarEstadistica();
        };

        UsuarioResponse usuario = authService.getCurrentUser(authentication.getName());
        String descripcionFiltros = describirFiltros(tipo, efectivos);
        LocalDateTime ahora = LocalDateTime.now();
        guardarAuditoria(tipo, usuario.id(), descripcionFiltros, ahora);

        return new ReporteResponse(tipo, tituloDe(tipo), ahora, usuario.nombre(),
                descripcionFiltros, datos.columnas(), datos.filas());
    }

    private Long resolverPatrocinadorFiltro(Authentication authentication, FiltrosReporteRequest filtros) {
        boolean privilegiado = tieneRol(authentication, "ADMIN") || tieneRol(authentication, "COMISIONADO");
        if (privilegiado) {
            return filtros.patrocinadorId();
        }
        UsuarioResponse usuario = authService.getCurrentUser(authentication.getName());
        Patrocinador propio = patrocinadorRepository.findByUsuarioId(usuario.id())
                .orElseThrow(() -> new ForbiddenException(
                        "Tu cuenta no está vinculada a ningún perfil de patrocinador."));
        return propio.getId();
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        String autoridad = "ROLE_" + rol;
        return authentication.getAuthorities().stream().anyMatch(a -> autoridad.equals(a.getAuthority()));
    }

    // ---------------------------------------------------------------
    // PATROCINIO — sin columna de Nivel (rediseño: el campo ya no es
    // significativo para un patrocinio nuevo, puede venir null).
    // ---------------------------------------------------------------
    private DatosReporte generarPatrocinio(FiltrosReporteRequest f) {
        List<Patrocinio> patrocinios =
                patrocinioRepository.buscarParaReporte(f.torneoId(), f.patrocinadorId(), f.desde(), f.hasta());

        List<String> columnas = List.of("Patrocinador", "Competencia", "Estado", "Desde", "Hasta");
        List<List<String>> filas = patrocinios.stream()
                .map(p -> List.of(
                        p.getPatrocinador().getNombre(),
                        nombreAlcance(p),
                        p.getEstado(),
                        p.getFechaInicio().toString(),
                        p.getFechaFin().toString()))
                .toList();
        return new DatosReporte(columnas, filas);
    }

    private String nombreAlcance(Patrocinio p) {
        if (p.getTorneo() != null) return "Torneo: " + p.getTorneo().getNombre();
        if (p.getTemporada() != null) return "Temporada: " + p.getTemporada().getNombre();
        if (p.getLiga() != null) return "Liga: " + p.getLiga().getNombre();
        return "—";
    }

    // ---------------------------------------------------------------
    // COMPETENCIA / RESULTADOS
    // ---------------------------------------------------------------
    private DatosReporte generarCompetencia(FiltrosReporteRequest f) {
        List<Torneo> torneos = resolverTorneos(f);

        List<String> columnas = List.of("Torneo", "Juego", "Ronda", "Equipo A", "Equipo B",
                "Marcador", "Ganador", "Estado", "Disputa");
        List<List<String>> filas = new ArrayList<>();
        for (Torneo t : torneos) {
            for (Partida p : partidaRepository.findByTorneoIdOrderByRondaAscOrdenAsc(t.getId())) {
                filas.add(List.of(
                        t.getNombre(),
                        t.getJuego().getNombre(),
                        String.valueOf(p.getRonda()),
                        p.getEquipoA() != null ? p.getEquipoA().getNombre() : "—",
                        p.getEquipoB() != null ? p.getEquipoB().getNombre() : "—",
                        marcador(p.getMarcadorA(), p.getMarcadorB()),
                        p.getGanador() != null ? p.getGanador().getNombre() : "—",
                        p.getEstado() != null ? p.getEstado().name() : "—",
                        describirDisputa(p.getId())));
            }
        }
        return new DatosReporte(columnas, filas);
    }

    private String marcador(Integer a, Integer b) {
        return (a == null || b == null) ? "—" : a + " - " + b;
    }

    private String describirDisputa(Long partidaId) {
        var disputas = disputaRepository.findByPartidaId(partidaId);
        if (disputas.isEmpty()) return "—";
        return disputas.stream()
                .filter(d -> d.getFechaResolucion() != null)
                .findFirst()
                .map(d -> "Resuelta: " + d.getDecision())
                .orElse("Pendiente");
    }

    // ---------------------------------------------------------------
    // AUDIENCIA
    // ---------------------------------------------------------------
    private DatosReporte generarAudiencia(FiltrosReporteRequest f) {
        List<Torneo> torneos = resolverTorneos(f);

        List<String> columnas = List.of("Torneo", "Pico espectadores", "Promedio espectadores",
                "Mensajes/min promedio", "Sentimiento predominante");
        List<List<String>> filas = new ArrayList<>();
        for (Torneo t : torneos) {
            for (TransmisionTwitch transmision : transmisionRepository.findByTorneoIdOrderByIniciadaEnDesc(t.getId())) {
                var audiencia = metricaAudienciaRepository.resumenPorTransmision(transmision.getId());
                var chat = metricaChatRepository.resumenPorTransmision(transmision.getId());
                filas.add(List.of(
                        t.getNombre(),
                        audiencia.getPico() != null ? audiencia.getPico().toString() : "—",
                        audiencia.getPromedio() != null ? String.format("%.1f", audiencia.getPromedio()) : "—",
                        chat.getMensajesPorMinutoPromedio() != null
                                ? String.format("%.1f", chat.getMensajesPorMinutoPromedio()) : "—",
                        sentimientoPredominante(transmision.getId())));
            }
        }
        return new DatosReporte(columnas, filas);
    }

    private String sentimientoPredominante(Long transmisionId) {
        return sentimientoRepository.contarPorClasificacionDeTransmision(transmisionId).stream()
                .max(Comparator.comparingLong(AnalisisSentimientoRepository.ConteoClasificacion::getCantidad))
                .map(AnalisisSentimientoRepository.ConteoClasificacion::getClasificacion)
                .orElse("Sin datos");
    }

    // ---------------------------------------------------------------
    // ESTADISTICA — Opción A: acumulado total, sin filtro de torneo/período.
    // ---------------------------------------------------------------
    private DatosReporte generarEstadistica() {
        List<String> columnas = List.of("Jugador", "Juego", "Victorias", "Derrotas", "Torneos jugados");
        List<List<String>> filas = estadisticaJugadorRepository.findAll().stream()
                .map(e -> List.of(
                        e.getUsuario().getNombre(),
                        e.getJuego().getNombre(),
                        e.getVictorias().toString(),
                        e.getDerrotas().toString(),
                        e.getTorneosJugados().toString()))
                .toList();
        return new DatosReporte(columnas, filas);
    }

    // ---------------------------------------------------------------
    // Compartido: resuelve la lista de torneos objetivo para Competencia/Audiencia.
    // El período se aplica siempre como filtro adicional. El filtro por
    // patrocinador ya NO se limita a alcance directo torneo_id (limitación
    // resuelta con el rediseño): usa PatrocinioService.resolverVigentePorTorneo,
    // la misma cascada Liga → Torneo del resto de la app — un torneo sin
    // patrocinio propio activo hereda el de su liga.
    //
    // Nota de rendimiento: recorre todos los torneos y resuelve la cascada
    // uno por uno (1-2 consultas cada uno). Aceptable a la escala de este
    // proyecto; con un catálogo de torneos mucho mayor convendría una
    // consulta agregada en vez de resolver torneo por torneo.
    // ---------------------------------------------------------------
    private List<Torneo> resolverTorneos(FiltrosReporteRequest f) {
        return resolverTorneosBase(f).stream()
                .filter(t -> f.desde() == null || !t.getFechaInicio().toLocalDate().isBefore(f.desde()))
                .filter(t -> f.hasta() == null || !t.getFechaInicio().toLocalDate().isAfter(f.hasta()))
                .toList();
    }

    private List<Torneo> resolverTorneosBase(FiltrosReporteRequest f) {
        if (f.patrocinadorId() != null) {
            return resolverTorneosDelPatrocinador(f.patrocinadorId(), f.torneoId());
        }
        if (f.torneoId() != null) {
            return torneoRepository.findById(f.torneoId()).map(List::of).orElse(List.of());
        }
        return torneoRepository.findAll();
    }

    private List<Torneo> resolverTorneosDelPatrocinador(Long patrocinadorId, Long torneoIdFiltro) {
        List<Torneo> candidatos = torneoIdFiltro != null
                ? torneoRepository.findById(torneoIdFiltro).map(List::of).orElse(List.of())
                : torneoRepository.findAll();

        return candidatos.stream()
                .filter(t -> patrocinioService.resolverVigentePorTorneo(t.getId())
                        .map(PatrocinioResponse::patrocinadorId)
                        .map(id -> id.equals(patrocinadorId))
                        .orElse(false))
                .toList();
    }

    // ---------------------------------------------------------------
    // Auditoría y metadatos
    // ---------------------------------------------------------------
    private void guardarAuditoria(TipoReporte tipo, Long usuarioId, String filtrosTexto, LocalDateTime fecha) {
        Usuario referencia = usuarioRepository.getReferenceById(usuarioId);
        reporteGeneradoRepository.save(ReporteGenerado.builder()
                .tipo(tipo)
                .usuario(referencia)
                .filtros(filtrosTexto)
                .fechaGeneracion(fecha)
                .build());
    }

    private String describirFiltros(TipoReporte tipo, FiltrosReporteRequest f) {
        List<String> partes = new ArrayList<>();
        partes.add(f.torneoId() != null ? "Torneo #" + f.torneoId() : "Todos los torneos");
        if (f.patrocinadorId() != null) partes.add("Patrocinador #" + f.patrocinadorId());
        if (f.desde() != null || f.hasta() != null) {
            partes.add("Período: " + (f.desde() != null ? f.desde() : "sin inicio")
                    + " a " + (f.hasta() != null ? f.hasta() : "sin fin"));
        }
        if (tipo == TipoReporte.ESTADISTICA) {
            partes.add("(filtros no aplican a este tipo — ver limitación conocida)");
        }
        return String.join(" · ", partes);
    }

    private String tituloDe(TipoReporte tipo) {
        return switch (tipo) {
            case COMPETENCIA -> "Reporte de competencias y resultados";
            case AUDIENCIA -> "Reporte de audiencia";
            case PATROCINIO -> "Reporte de patrocinio";
            case ESTADISTICA -> "Reporte de estadísticas";
        };
    }

    private record DatosReporte(List<String> columnas, List<List<String>> filas) {}
}