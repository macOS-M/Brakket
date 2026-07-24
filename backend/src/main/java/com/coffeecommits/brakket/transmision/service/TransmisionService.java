package com.coffeecommits.brakket.transmision.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.config.StreamsProperties;
import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.transmision.dto.TransmisionesResponse;
import com.coffeecommits.brakket.transmision.dto.TransmisionesResponse.TransmisionResponse;
import com.coffeecommits.brakket.transmision.dto.TransmisionesResponse.VodResponse;
import com.coffeecommits.brakket.twitch.model.IncidenteIntegracionTwitch;
import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.CanalOficialTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.IncidenteIntegracionTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import com.coffeecommits.brakket.twitch.service.TwitchUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Estado en vivo de las transmisiones de Brakket (RF-35, RNF-02/15/22).
 *
 * <p>Caché de TTL corto sobre Helix: 25s de caché + 30s de polling del front
 * dan un peor caso de ~55s, dentro del minuto que exige RNF-02, y una sola
 * llamada batcheada por ventana respeta la cuota del token con margen.</p>
 *
 * <p>Además del caché TTL se conserva un last-known-good aparte: cuando Twitch
 * no responde, el TTL ya expiró (por eso se fue a pedir datos frescos), así
 * que la degradación sirve la última respuesta buena con estado DESCONOCIDO y
 * su {@code actualizadoEn} real para que la UI diga "última info de hace X".</p>
 */
@Service
@Slf4j
public class TransmisionService {
    private static final Duration ESPERA_MAXIMA_VOD = Duration.ofSeconds(2);
    private static final Duration THROTTLE_INCIDENTES = Duration.ofMinutes(5);

    private final CanalOficialTwitchRepository canalRepository;
    private final TransmisionTwitchRepository transmisionRepository;
    private final IncidenteIntegracionTwitchRepository incidenteRepository;
    private final TwitchProperties twitchProperties;
    private final StreamsProperties streamsProperties;
    private final Map<PlataformaTransmision, StreamProvider> providers;

    private volatile TransmisionesResponse cacheRespuesta;
    private volatile Instant cacheEn = Instant.EPOCH;
    private volatile TransmisionesResponse ultimaBuena;
    private volatile Instant ultimoIncidenteEn = Instant.EPOCH;
    private final Map<String, EntradaVod> vodCache = new ConcurrentHashMap<>();

    public TransmisionService(CanalOficialTwitchRepository canalRepository,
                              TransmisionTwitchRepository transmisionRepository,
                              IncidenteIntegracionTwitchRepository incidenteRepository,
                              TwitchProperties twitchProperties,
                              StreamsProperties streamsProperties,
                              List<StreamProvider> providers) {
        this.canalRepository = canalRepository;
        this.transmisionRepository = transmisionRepository;
        this.incidenteRepository = incidenteRepository;
        this.twitchProperties = twitchProperties;
        this.streamsProperties = streamsProperties;
        this.providers = providers.stream()
                .collect(Collectors.toMap(StreamProvider::plataforma, Function.identity()));
    }

    public TransmisionesResponse listar() {
        TransmisionesResponse cacheada = cacheRespuesta;
        if (cacheada != null && Instant.now().isBefore(
                cacheEn.plusSeconds(streamsProperties.getCacheTtlSegundos()))) {
            return cacheada;
        }
        return consultar();
    }

    private synchronized TransmisionesResponse consultar() {
        // Segundo chequeo dentro del lock: si otro request ya refrescó, reusar.
        TransmisionesResponse cacheada = cacheRespuesta;
        if (cacheada != null && Instant.now().isBefore(
                cacheEn.plusSeconds(streamsProperties.getCacheTtlSegundos()))) {
            return cacheada;
        }
        List<Fuente> fuentes = fuentes();
        if (fuentes.isEmpty()) {
            return cachear(new TransmisionesResponse(List.of(), LocalDateTime.now(), false));
        }
        StreamProvider provider = providers.get(PlataformaTransmision.TWITCH);
        List<String> logins = fuentes.stream().map(Fuente::login).toList();
        try {
            Map<String, StreamProvider.CanalStream> canales = provider.getChannels(logins).stream()
                    .collect(Collectors.toMap(StreamProvider.CanalStream::login, Function.identity()));
            Map<String, StreamProvider.StreamEnVivo> directos = provider.getLiveStreams(logins).stream()
                    .collect(Collectors.toMap(StreamProvider.StreamEnVivo::login, Function.identity()));

            List<TransmisionResponse> transmisiones = new ArrayList<>();
            for (Fuente fuente : fuentes) {
                StreamProvider.CanalStream canal = canales.get(fuente.login());
                if (canal == null) {
                    // El handle no existe en Twitch: mejor omitirlo que pintar
                    // una tarjeta rota; RF-34 ya valida el canal al registrarlo.
                    log.warn("El canal '{}' no existe en Twitch; se omite de /transmisiones.", fuente.login());
                    continue;
                }
                transmisiones.add(aResponse(fuente, canal, directos.get(fuente.login())));
            }
            TransmisionesResponse respuesta =
                    new TransmisionesResponse(transmisiones, LocalDateTime.now(), false);
            ultimaBuena = respuesta;
            return cachear(respuesta);
        } catch (TwitchUnavailableException | BusinessException ex) {
            registrarIncidente(ex.getMessage());
            return cachear(respuestaDegradada(fuentes));
        }
    }

    private TransmisionResponse aResponse(Fuente fuente, StreamProvider.CanalStream canal,
                                          StreamProvider.StreamEnVivo directo) {
        if (directo != null) {
            return new TransmisionResponse(PlataformaTransmision.TWITCH.name(), canal.login(),
                    canal.nombreMostrado(), canal.avatarUrl(), urlCanal(canal.login()), "EN_VIVO",
                    directo.titulo(), directo.espectadores(), directo.thumbnailUrl(),
                    directo.categoria(), directo.idioma(), directo.iniciadoEn(),
                    fuente.destacada(), fuente.torneoId(), fuente.nombreTorneo(), null);
        }
        return new TransmisionResponse(PlataformaTransmision.TWITCH.name(), canal.login(),
                canal.nombreMostrado(), canal.avatarUrl(), urlCanal(canal.login()), "OFFLINE",
                null, null, canal.offlineImageUrl(), null, null, null,
                fuente.destacada(), fuente.torneoId(), fuente.nombreTorneo(),
                vodPara(canal.id()));
    }

    /**
     * Último VOD del canal, SIN bloquear la tarjeta offline: /videos no se
     * puede batchear, así que se consulta aparte, con caché más largo y una
     * espera acotada. Si tarda o falla, la tarjeta sale sin VOD y el resultado
     * (cuando llegue) queda cacheado para el siguiente refresco.
     */
    private VodResponse vodPara(String userId) {
        EntradaVod entrada = vodCache.get(userId);
        if (entrada != null && Instant.now().isBefore(
                entrada.cargadoEn().plusSeconds(streamsProperties.getVodCacheSegundos()))) {
            return entrada.vod();
        }
        StreamProvider provider = providers.get(PlataformaTransmision.TWITCH);
        CompletableFuture<StreamProvider.VodInfo> futuro =
                CompletableFuture.supplyAsync(() -> provider.getLatestVod(userId));
        try {
            VodResponse vod = aVodResponse(futuro.get(ESPERA_MAXIMA_VOD.toMillis(), TimeUnit.MILLISECONDS));
            vodCache.put(userId, new EntradaVod(vod, Instant.now()));
            return vod;
        } catch (TimeoutException ex) {
            futuro.whenComplete((vodInfo, error) -> {
                if (error == null) vodCache.put(userId, new EntradaVod(aVodResponse(vodInfo), Instant.now()));
            });
            return entrada == null ? null : entrada.vod();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            log.warn("No se pudo obtener el último VOD del canal {}: {}", userId, ex.getMessage());
            return entrada == null ? null : entrada.vod();
        }
    }

    private VodResponse aVodResponse(StreamProvider.VodInfo vod) {
        return vod == null ? null : new VodResponse(vod.id(), vod.url(), vod.titulo(),
                vod.thumbnailUrl(), vod.duracion(), vod.publicadoEn());
    }

    /**
     * RNF-15: con Twitch caído se sirve el last-known-good marcado como
     * DESCONOCIDO (nunca un EN_VIVO viejo como si fuera actual) y, si nunca
     * hubo respuesta buena, al menos las tarjetas de canal desde la BD.
     */
    private TransmisionesResponse respuestaDegradada(List<Fuente> fuentes) {
        TransmisionesResponse base = ultimaBuena;
        if (base != null) {
            List<TransmisionResponse> transmisiones = base.transmisiones().stream()
                    .map(t -> new TransmisionResponse(t.plataforma(), t.loginCanal(), t.nombreCanal(),
                            t.avatarUrl(), t.urlCanal(), "DESCONOCIDO", t.titulo(), null,
                            t.thumbnailUrl(), t.categoria(), t.idioma(), t.iniciadaEn(),
                            t.destacada(), t.torneoId(), t.nombreTorneo(), t.vod()))
                    .toList();
            return new TransmisionesResponse(transmisiones, base.actualizadoEn(), true);
        }
        List<TransmisionResponse> transmisiones = fuentes.stream()
                .map(f -> new TransmisionResponse(PlataformaTransmision.TWITCH.name(), f.login(),
                        f.login(), null, urlCanal(f.login()), "DESCONOCIDO", null, null, null,
                        null, null, null, f.destacada(), f.torneoId(), f.nombreTorneo(), null))
                .toList();
        return new TransmisionesResponse(transmisiones, null, true);
    }

    /**
     * Canales a consultar: el oficial (RF-34, con la env var como semilla) y,
     * con el feature flag multi-fuente, las transmisiones registradas (RF-35).
     * Siempre es una lista: encender el flag no cambia ningún código.
     */
    private List<Fuente> fuentes() {
        Map<String, Fuente> porLogin = new LinkedHashMap<>();
        String oficial = canalRepository.findFirstByActivoTrue()
                .map(c -> c.getLoginCanal())
                .orElse(twitchProperties.getChannel());
        if (oficial != null && !oficial.isBlank()) {
            String login = oficial.trim().toLowerCase(Locale.ROOT);
            porLogin.put(login, new Fuente(login, true, null, null));
        }
        if (streamsProperties.isMultiSourceEnabled()) {
            for (TransmisionTwitch t : transmisionRepository.findActivasConCanalYTorneo()) {
                if (t.getPlataforma() != PlataformaTransmision.TWITCH) continue; // sin provider aún
                String login = t.getLoginCanal() != null ? t.getLoginCanal()
                        : (t.getCanal() == null ? null : t.getCanal().getLoginCanal());
                if (login == null || login.isBlank()) continue;
                login = login.trim().toLowerCase(Locale.ROOT);
                porLogin.putIfAbsent(login, new Fuente(login, t.isDestacada(),
                        t.getTorneo() == null ? null : t.getTorneo().getId(),
                        t.getTorneo() == null ? null : t.getTorneo().getNombre()));
            }
        }
        return List.copyOf(porLogin.values());
    }

    private TransmisionesResponse cachear(TransmisionesResponse respuesta) {
        cacheRespuesta = respuesta;
        cacheEn = Instant.now();
        return respuesta;
    }

    /** RNF-21, con throttle: una caída larga no debe insertar una fila cada 25s. */
    private void registrarIncidente(String detalle) {
        if (Instant.now().isBefore(ultimoIncidenteEn.plus(THROTTLE_INCIDENTES))) return;
        ultimoIncidenteEn = Instant.now();
        try {
            incidenteRepository.save(IncidenteIntegracionTwitch.builder()
                    .canal(canalRepository.findFirstByActivoTrue().orElse(null))
                    .tipo("CONEXION")
                    .detalle(detalle == null ? "Twitch no disponible." : detalle)
                    .ocurridoEn(LocalDateTime.now()).build());
        } catch (Exception ex) {
            log.warn("No se pudo registrar el incidente de integración: {}", ex.getMessage());
        }
    }

    private String urlCanal(String login) {
        return "https://www.twitch.tv/" + login;
    }

    private record Fuente(String login, boolean destacada, Long torneoId, String nombreTorneo) {}

    private record EntradaVod(VodResponse vod, Instant cargadoEn) {}
}
