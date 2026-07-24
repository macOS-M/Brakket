package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.transmision.service.StreamProvider;
import com.coffeecommits.brakket.twitch.model.IncidenteIntegracionTwitch;
import com.coffeecommits.brakket.twitch.model.MetricaAudiencia;
import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.IncidenteIntegracionTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Muestreo periódico de audiencia (RF-36, RNF-02/15/22).
 *
 * <p>Cada tick toma las transmisiones registradas con período de captura
 * abierto, consulta sus directos en UNA llamada batcheada a Helix y guarda una
 * muestra por transmisión en vivo. Cuando el directo termina, cierra el
 * período ({@code finalizadaEn}); si Twitch no responde, registra el incidente
 * y deja el hueco sin datos — nunca se inventan valores. Los indicadores
 * (pico, promedio, duración) se calculan sobre las muestras guardadas.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MuestreoAudienciaService {
    private static final Duration THROTTLE_INCIDENTES = Duration.ofMinutes(5);

    private final TransmisionTwitchRepository transmisionRepository;
    private final MetricaAudienciaRepository metricaRepository;
    private final IncidenteIntegracionTwitchRepository incidenteRepository;
    private final TwitchProperties twitchProperties;
    private final List<StreamProvider> providers;

    private volatile Instant ultimoIncidenteEn = Instant.EPOCH;

    /**
     * Ticks consecutivos sin directo por transmisión: una reconexión de OBS
     * hace desaparecer el stream de Helix por un instante y cerrar el período
     * al primer fallo lo clausuraba definitivamente. Se cierra al segundo.
     */
    private final Map<Long, Integer> ticksSinDirecto = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int TICKS_PARA_CERRAR = 2;

    /**
     * Un tick por minuto mantiene el desfase de métricas bajo el minuto que
     * exige RNF-02 y consume una única request de la cuota por tick (RNF-22).
     */
    @Scheduled(fixedDelayString = "${brakket.streams.muestreo-intervalo-ms:60000}",
            initialDelayString = "${brakket.streams.muestreo-espera-inicial-ms:15000}")
    @Transactional
    public void muestrear() {
        if (!twitchProperties.isConfigured()) {
            // Sin credenciales (test/CI) el muestreador existe pero no corre.
            return;
        }
        List<TransmisionTwitch> abiertas = transmisionRepository.findAbiertasParaMuestreo().stream()
                .filter(t -> t.getPlataforma() == PlataformaTransmision.TWITCH)
                .filter(t -> login(t) != null)
                .toList();
        if (abiertas.isEmpty()) {
            return;
        }
        StreamProvider provider = providers.stream()
                .filter(p -> p.plataforma() == PlataformaTransmision.TWITCH)
                .findFirst().orElseThrow();
        List<String> logins = abiertas.stream().map(this::login).distinct().toList();

        Map<String, StreamProvider.StreamEnVivo> directos;
        try {
            directos = provider.getLiveStreams(logins).stream()
                    .collect(Collectors.toMap(StreamProvider.StreamEnVivo::login, Function.identity()));
        } catch (TwitchUnavailableException | BusinessException ex) {
            // Hueco sin datos: se registra el incidente y este tick no escribe
            // ninguna muestra (el ERS prohíbe inventar valores).
            registrarIncidente(ex.getMessage());
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (TransmisionTwitch transmision : abiertas) {
            StreamProvider.StreamEnVivo directo = directos.get(login(transmision));
            if (directo != null) {
                ticksSinDirecto.remove(transmision.getId());
                registrarMuestra(transmision, directo, ahora);
            } else if ("EN_VIVO".equals(transmision.getEstado())) {
                int fallos = ticksSinDirecto.merge(transmision.getId(), 1, Integer::sum);
                if (fallos >= TICKS_PARA_CERRAR) {
                    // El directo terminó de verdad: se cierra el período de captura.
                    ticksSinDirecto.remove(transmision.getId());
                    transmision.setEstado("FINALIZADA");
                    transmision.setFinalizadaEn(ahora);
                    log.info("Transmisión {} finalizada; período de captura cerrado.", transmision.getId());
                }
            }
        }
    }

    private void registrarMuestra(TransmisionTwitch transmision,
                                  StreamProvider.StreamEnVivo directo, LocalDateTime ahora) {
        metricaRepository.save(MetricaAudiencia.builder()
                .transmisionTwitch(transmision)
                .fechaHora(ahora)
                .espectadores(directo.espectadores())
                .build());
        transmision.setEstado("EN_VIVO");
        transmision.setVerificadaEn(ahora);
        if (transmision.getTwitchStreamId() == null) {
            transmision.setTwitchStreamId(directo.id());
        }
        if (transmision.getIniciadaEn() == null) {
            transmision.setIniciadaEn(directo.iniciadoEn());
        }
    }

    private String login(TransmisionTwitch t) {
        String login = t.getLoginCanal() != null ? t.getLoginCanal()
                : (t.getCanal() == null ? null : t.getCanal().getLoginCanal());
        return login == null || login.isBlank() ? null : login.trim().toLowerCase(Locale.ROOT);
    }

    /** RNF-21 con throttle: una caída larga no inserta una fila por tick. */
    private void registrarIncidente(String detalle) {
        if (Instant.now().isBefore(ultimoIncidenteEn.plus(THROTTLE_INCIDENTES))) {
            return;
        }
        ultimoIncidenteEn = Instant.now();
        incidenteRepository.save(IncidenteIntegracionTwitch.builder()
                .tipo("MUESTREO")
                .detalle(detalle == null ? "Twitch no disponible durante el muestreo." : detalle)
                .ocurridoEn(LocalDateTime.now()).build());
        log.warn("Muestreo de audiencia sin respuesta de Twitch: {}", detalle);
    }
}
