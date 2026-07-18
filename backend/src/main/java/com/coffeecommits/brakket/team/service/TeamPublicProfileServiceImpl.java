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
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
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

    public TeamPublicProfileServiceImpl(EquipoRepository equipoRepository,
                                        MiembroEquipoRepository miembroRepository,
                                        EquipoRedSocialRepository redSocialRepository,
                                        InscripcionRepository inscripcionRepository,
                                        EstadisticaJugadorRepository estadisticaRepository) {
        this.equipoRepository = equipoRepository;
        this.miembroRepository = miembroRepository;
        this.redSocialRepository = redSocialRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.estadisticaRepository = estadisticaRepository;
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
                        i.getTorneo().getNombre(), i.getTorneo().getEstado(), i.getTorneo().getFechaInicio(),
                        i.getTorneo().getFechaFin(), i.getEstado()))
                .toList();

        List<EstadisticaJugador> estadisticas = juegoSeleccionado == null ? List.of()
                : plantilla.stream()
                    .flatMap(m -> estadisticaRepository.findByUsuarioIdAndJuegoId(m.usuarioId(), juegoSeleccionado).stream())
                    .toList();
        var resumen = new PerfilEquipoPublicoResponse.EstadisticasGenerales(
                estadisticas.stream().mapToInt(EstadisticaJugador::getVictorias).sum(),
                estadisticas.stream().mapToInt(EstadisticaJugador::getDerrotas).sum(),
                estadisticas.stream().mapToInt(EstadisticaJugador::getTorneosJugados).sum(),
                !estadisticas.isEmpty());

        return new PerfilEquipoPublicoResponse(equipo.getId(), equipo.getNombre(), equipo.getLogo(),
                equipo.getDescripcion(), equipo.getEstado(), equipo.getCapitan().getId(), juegoSeleccionado,
                equipo.getJuego() == null ? null : equipo.getJuego().getNombre(),
                redSocialRepository.findByEquipoId(equipo.getId()).stream().map(r -> r.getUrl()).toList(),
                plantilla, torneos, resumen);
    }
}
