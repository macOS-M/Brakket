package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.statistics.model.EstadisticaJugador;
import com.coffeecommits.brakket.statistics.repository.EstadisticaJugadorRepository;
import com.coffeecommits.brakket.team.dto.EquipoResumenPublicoResponse;
import com.coffeecommits.brakket.team.dto.PerfilEquipoPublicoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.repository.EquipoRedSocialRepository;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeamPublicProfileServiceImpl implements TeamPublicProfileService {

    private final EquipoRepository equipoRepository;
    private final MiembroEquipoRepository miembroRepository;
    private final EquipoRedSocialRepository redSocialRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstadisticaJugadorRepository estadisticaRepository;
    private final PartidaRepository partidaRepository;

    public TeamPublicProfileServiceImpl(EquipoRepository equipoRepository,
                                        MiembroEquipoRepository miembroRepository,
                                        EquipoRedSocialRepository redSocialRepository,
                                        InscripcionRepository inscripcionRepository,
                                        EstadisticaJugadorRepository estadisticaRepository,
                                        PartidaRepository partidaRepository) {
        this.equipoRepository = equipoRepository;
        this.miembroRepository = miembroRepository;
        this.redSocialRepository = redSocialRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.estadisticaRepository = estadisticaRepository;
        this.partidaRepository = partidaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilEquipoPublicoResponse consultarPerfil(Long equipoId, Long juegoId) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));
        return construirPerfil(equipo, juegoId);
    }

    /**
     * El listado NO arma el perfil completo (miembros, torneos y estadísticas
     * por integrante multiplicaban las queries por equipo): trae los equipos
     * con su juego en una query y los conteos de miembros activos en otra.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EquipoResumenPublicoResponse> buscarEquipos(String criterio) {
        List<Equipo> equipos = equipoRepository.buscarPorNombreConJuego(
                criterio == null ? "" : criterio.trim());
        if (equipos.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> activosPorEquipo = miembroRepository
                .contarActivosPorEquipo(equipos.stream().map(Equipo::getId).toList()).stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]));

        return equipos.stream()
                .map(equipo -> new EquipoResumenPublicoResponse(
                        equipo.getId(),
                        equipo.getNombre(),
                        equipo.getLogo(),
                        equipo.getJuego() == null ? null : equipo.getJuego().getNombre(),
                        equipo.getJuegos().stream().map(j -> j.getNombre()).toList(),
                        activosPorEquipo.getOrDefault(equipo.getId(), 0L)))
                .toList();
    }

    private PerfilEquipoPublicoResponse construirPerfil(Equipo equipo, Long juegoId) {
        Long juegoSeleccionado = juegoId != null ? juegoId
                : equipo.getJuego() == null ? null : equipo.getJuego().getId();

        var plantilla = miembroRepository.findByEquipoIdAndEstado(equipo.getId(), "ACTIVO").stream()
                .map(m -> new PerfilEquipoPublicoResponse.IntegrantePublico(
                        m.getUsuario().getId(), m.getUsuario().getNombre(), m.getRol(), m.getFechaUnion()))
                .toList();

        var torneos = inscripcionRepository.findByEquipoId(equipo.getId()).stream()
                .map(i -> new PerfilEquipoPublicoResponse.TorneoRelacionado(i.getTorneo().getId(),
                        i.getTorneo().getNombre(), i.getTorneo().getEstado().name(), i.getTorneo().getFechaInicio(),
                        i.getTorneo().getFechaFin(), i.getEstado()))
                .toList();

        // Primero los números REALES del motor de torneos: victorias y
        // derrotas contadas sobre las partidas jugadas (los byes no cuentan)
        // y torneos donde el equipo efectivamente compitió. Las estadísticas
        // manuales por jugador (RF-31) quedan como respaldo para equipos que
        // aún no jugaron nada dentro de la plataforma.
        long victorias = partidaRepository.victoriasDe(equipo.getId());
        long derrotas = partidaRepository.derrotasDe(equipo.getId());
        long torneosCompetidos = torneos.stream()
                .filter(t -> EstadoTorneo.EN_CURSO.name().equals(t.estado())
                        || EstadoTorneo.FINALIZADO.name().equals(t.estado()))
                .count();

        PerfilEquipoPublicoResponse.EstadisticasGenerales resumen;
        if (victorias + derrotas > 0) {
            resumen = new PerfilEquipoPublicoResponse.EstadisticasGenerales(
                    (int) victorias, (int) derrotas, (int) torneosCompetidos, true);
        } else {
            List<EstadisticaJugador> estadisticas = juegoSeleccionado == null ? List.of()
                    : plantilla.stream()
                        .flatMap(m -> estadisticaRepository.findByUsuarioIdAndJuegoId(m.usuarioId(), juegoSeleccionado).stream())
                        .toList();
            resumen = new PerfilEquipoPublicoResponse.EstadisticasGenerales(
                    estadisticas.stream().mapToInt(EstadisticaJugador::getVictorias).sum(),
                    estadisticas.stream().mapToInt(EstadisticaJugador::getDerrotas).sum(),
                    estadisticas.stream().mapToInt(EstadisticaJugador::getTorneosJugados).sum(),
                    !estadisticas.isEmpty());
        }

        return new PerfilEquipoPublicoResponse(equipo.getId(), equipo.getNombre(), equipo.getLogo(),
                equipo.getBannerUrl(), equipo.getDescripcion(), equipo.getSitioWeb(),
                equipo.getVideoUrl(), equipo.getEstado(), equipo.getCapitan().getId(), juegoSeleccionado,
                equipo.getJuego() == null ? null : equipo.getJuego().getNombre(),
                equipo.getJuegos().stream().map(j -> j.getId()).toList(),
                equipo.getJuegos().stream().map(j -> j.getNombre()).toList(),
                redSocialRepository.findByEquipoId(equipo.getId()).stream().map(r -> r.getUrl()).toList(),
                plantilla, torneos, resumen);
    }
}
