package com.coffeecommits.brakket.twitch.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.twitch.dto.AsociarTransmisionRequest;
import com.coffeecommits.brakket.twitch.dto.CanalTwitchResponse;
import com.coffeecommits.brakket.twitch.dto.ConfigurarCanalTwitchRequest;
import com.coffeecommits.brakket.twitch.dto.MetricasTransmisionResponse;
import com.coffeecommits.brakket.twitch.dto.TransmisionTwitchResponse;
import com.coffeecommits.brakket.twitch.model.CanalOficialTwitch;
import com.coffeecommits.brakket.twitch.model.EstadoIntegracionTwitch;
import com.coffeecommits.brakket.twitch.model.IncidenteIntegracionTwitch;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.CanalOficialTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.IncidenteIntegracionTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CanalTwitchService {
    private final CanalOficialTwitchRepository canalRepository;
    private final TransmisionTwitchRepository transmisionRepository;
    private final IncidenteIntegracionTwitchRepository incidenteRepository;
    private final MetricaAudienciaRepository metricaRepository;
    private final TorneoRepository torneoRepository;
    private final PartidaRepository partidaRepository;
    private final TwitchGateway twitchGateway;
    private final TwitchProperties properties;

    @Transactional(readOnly = true)
    public CanalTwitchResponse obtener() {
        // El último canal registrado, esté ACTIVO o PENDIENTE: si solo se
        // devolviera el activo, un canal guardado sin credenciales
        // "desaparecía" al recargar y el botón Validar quedaba inservible.
        return canalRepository.findFirstByOrderByIdDesc().map(this::response)
                .orElse(new CanalTwitchResponse(null, null, normalizar(properties.getChannel()),
                        null, url(normalizar(properties.getChannel())), "SIN_CONFIGURAR", false,
                        null, null, properties.isConfigured()));
    }

    @Transactional
    public CanalTwitchResponse configurar(ConfigurarCanalTwitchRequest request) {
        String login = normalizar(request.canal());
        if (canalRepository.existsByLoginCanalIgnoreCaseAndActivoTrue(login)) {
            throw new BusinessException("El canal ya está registrado como canal oficial activo.");
        }
        canalRepository.findFirstByActivoTrue().ifPresent(anterior -> {
            anterior.setActivo(false);
            anterior.setActualizadoEn(LocalDateTime.now());
        });
        LocalDateTime ahora = LocalDateTime.now();
        CanalOficialTwitch canal = canalRepository.save(CanalOficialTwitch.builder()
                .loginCanal(login).urlCanal(url(login)).estado(EstadoIntegracionTwitch.PENDIENTE)
                .activo(false).creadoEn(ahora).actualizadoEn(ahora).build());
        if (!properties.isConfigured()) {
            canal.setUltimoError("Pendiente de configurar las credenciales de Twitch.");
            return response(canalRepository.save(canal));
        }
        return validarEntidad(canal);
    }

    @Transactional
    public CanalTwitchResponse validar() {
        CanalOficialTwitch canal = canalRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No hay un canal configurado."));
        return validarEntidad(canal);
    }

    @Transactional
    public TransmisionTwitchResponse asociar(AsociarTransmisionRequest request) {
        if (request.torneoId() == null && request.partidaId() == null)
            throw new IllegalArgumentException("Debe indicar un torneo o una partida.");
        CanalOficialTwitch canal = canalRepository.findFirstByActivoTrue()
                .orElseThrow(() -> new BusinessException("No existe un canal oficial activo."));
        Torneo torneo = request.torneoId() == null ? null : torneoRepository.findById(request.torneoId())
                .orElseThrow(() -> new ResourceNotFoundException("El torneo no existe."));
        Partida partida = request.partidaId() == null ? null : partidaRepository.findById(request.partidaId())
                .orElseThrow(() -> new ResourceNotFoundException("La partida no existe."));
        if (partida != null && torneo != null && !partida.getTorneo().getId().equals(torneo.getId()))
            throw new BusinessException("La partida no pertenece al torneo indicado.");
        TwitchGateway.StreamInfo live = twitchGateway.findLiveStream(canal.getLoginCanal());
        TransmisionTwitch entity = transmisionRepository.save(TransmisionTwitch.builder()
                .canal(canal).torneo(torneo).partida(partida)
                .loginCanal(canal.getLoginCanal())
                .twitchStreamId(live == null ? null : live.id())
                .estado(live == null ? "SIN_DATOS_EN_VIVO" : "EN_VIVO")
                .verificadaEn(live == null ? null : LocalDateTime.now())
                .iniciadaEn(live == null ? null : live.startedAt()).creadaEn(LocalDateTime.now()).build());
        return new TransmisionTwitchResponse(entity.getId(), entity.getTwitchStreamId(),
                torneo == null ? null : torneo.getId(), partida == null ? null : partida.getId(),
                entity.getEstado(), entity.getIniciadaEn());
    }

    private CanalTwitchResponse validarEntidad(CanalOficialTwitch canal) {
        try {
            TwitchGateway.ChannelInfo info = twitchGateway.findChannel(canal.getLoginCanal());
            if (info == null) {
                canal.setEstado(EstadoIntegracionTwitch.ERROR);
                canal.setUltimoError("El canal no existe en Twitch.");
                canal.setActivo(false);
                throw new ResourceNotFoundException("El canal no existe en Twitch.");
            }
            canal.setTwitchUsuarioId(info.id());
            canal.setNombreMostrado(info.displayName());
            canal.setEstado(EstadoIntegracionTwitch.ACTIVO);
            canal.setActivo(true);
            canal.setUltimoError(null);
            canal.setUltimaValidacion(LocalDateTime.now());
            canal.setActualizadoEn(LocalDateTime.now());
            return response(canalRepository.save(canal));
        } catch (TwitchUnavailableException ex) {
            canal.setEstado(EstadoIntegracionTwitch.NO_DISPONIBLE);
            canal.setActivo(false);
            canal.setUltimoError(ex.getMessage());
            canal.setActualizadoEn(LocalDateTime.now());
            canalRepository.save(canal);
            incidenteRepository.save(IncidenteIntegracionTwitch.builder().canal(canal)
                    .tipo("CONEXION").detalle(ex.getMessage()).ocurridoEn(LocalDateTime.now()).build());
            return response(canal);
        }
    }

    /** RF-34: transmisiones con periodo de captura abierto (las que el muestreo sigue). */
    @Transactional(readOnly = true)
    public List<TransmisionTwitchResponse> listarAbiertas() {
        return transmisionRepository.findAbiertasParaMuestreo().stream()
                .map(t -> new TransmisionTwitchResponse(
                        t.getId(), t.getTwitchStreamId(),
                        t.getTorneo() == null ? null : t.getTorneo().getId(),
                        t.getPartida() == null ? null : t.getPartida().getId(),
                        t.getEstado(), t.getIniciadaEn()))
                .toList();
    }

    /**
     * Indicadores básicos de RF-36 sobre las muestras capturadas. La duración
     * corre desde el inicio del directo hasta su cierre (o hasta ahora si
     * sigue en vivo).
     */
    @Transactional(readOnly = true)
    public MetricasTransmisionResponse metricas(Long transmisionId) {
        TransmisionTwitch transmision = transmisionRepository.findById(transmisionId)
                .orElseThrow(() -> new ResourceNotFoundException("La transmisión no existe."));
        var resumen = metricaRepository.resumenPorTransmision(transmisionId);
        Long duracion = transmision.getIniciadaEn() == null ? null
                : java.time.Duration.between(transmision.getIniciadaEn(),
                        transmision.getFinalizadaEn() == null ? LocalDateTime.now()
                                : transmision.getFinalizadaEn()).toMinutes();
        return new MetricasTransmisionResponse(transmision.getId(), transmision.getEstado(),
                resumen.getMuestras() == null ? 0 : resumen.getMuestras(), resumen.getPico(),
                resumen.getPromedio(), duracion, transmision.getIniciadaEn(),
                transmision.getFinalizadaEn(), resumen.getUltimaMuestra());
    }

    private CanalTwitchResponse response(CanalOficialTwitch c) {
        return new CanalTwitchResponse(c.getId(), c.getTwitchUsuarioId(), c.getLoginCanal(),
                c.getNombreMostrado(), c.getUrlCanal(), c.getEstado().name(), c.isActivo(),
                c.getUltimoError(), c.getUltimaValidacion(), properties.isConfigured());
    }
    private String normalizar(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim().replaceAll("/+$", "");
        int slash = clean.lastIndexOf('/');
        return (slash >= 0 ? clean.substring(slash + 1) : clean).toLowerCase(Locale.ROOT);
    }
    private String url(String login) { return login == null ? null : "https://www.twitch.tv/" + login; }
}
