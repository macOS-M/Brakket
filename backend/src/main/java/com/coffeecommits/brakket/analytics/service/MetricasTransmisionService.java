package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.dto.AgrupacionMetrica;
import com.coffeecommits.brakket.analytics.dto.ClaveSerie;
import com.coffeecommits.brakket.analytics.dto.SeriesTransmisionResponse;
import com.coffeecommits.brakket.analytics.dto.TransmisionAnalizableResponse;
import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.twitch.model.MetricaAudiencia;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.OrigenMetrica;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RF-37: consulta de métricas de una transmisión por período y rango horario.
 *
 * <p>Complementa a RF-36, que solo expone un resumen de toda la transmisión sin
 * filtros. Acá se devuelven las series completas de audiencia, chat y sentimiento,
 * acotadas por rango y agrupadas crudas o por hora.
 */
@Service
public class MetricasTransmisionService {

    private static final DateTimeFormatter ETIQUETA_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // Un rango sin cotas se traduce a estos límites en vez de mandar null al
    // repositorio: Postgres no puede inferir el tipo de un parámetro suelto en
    // "(:desde is null or ...)" y la consulta falla al ejecutarse.
    private static final LocalDateTime SIN_COTA_INFERIOR = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime SIN_COTA_SUPERIOR = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private final TransmisionTwitchRepository transmisionRepository;
    private final MetricaAudienciaRepository audienciaRepository;
    private final MetricaChatRepository chatRepository;
    private final AnalisisSentimientoRepository sentimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final int intervaloSegundos;

    public MetricasTransmisionService(TransmisionTwitchRepository transmisionRepository,
                                      MetricaAudienciaRepository audienciaRepository,
                                      MetricaChatRepository chatRepository,
                                      AnalisisSentimientoRepository sentimientoRepository,
                                      UsuarioRepository usuarioRepository,
                                      @Value("${brakket.streams.muestreo-intervalo-ms:60000}") long intervaloMs) {
        this.transmisionRepository = transmisionRepository;
        this.audienciaRepository = audienciaRepository;
        this.chatRepository = chatRepository;
        this.sentimientoRepository = sentimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.intervaloSegundos = (int) Math.max(1, intervaloMs / 1000);
    }

    /** Transmisiones que el usuario puede consultar, con su conteo de muestras. */
    @Transactional(readOnly = true)
    public List<TransmisionAnalizableResponse> catalogo(String correo, boolean esAdmin) {
        Usuario usuario = esAdmin ? null : usuarioActual(correo);
        List<TransmisionTwitch> visibles = transmisionRepository.findParaAnalitica().stream()
                .filter(t -> esAdmin || puedeVer(t, usuario))
                .toList();
        if (visibles.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> muestras = new HashMap<>();
        List<Long> ids = visibles.stream().map(TransmisionTwitch::getId).toList();
        for (Object[] fila : audienciaRepository.contarMuestrasPorTransmision(ids)) {
            muestras.put((Long) fila[0], (Long) fila[1]);
        }

        return visibles.stream().map(t -> {
            Torneo torneo = torneoDe(t);
            return new TransmisionAnalizableResponse(t.getId(), etiquetaDe(t, torneo),
                    torneo == null ? null : torneo.getId(),
                    torneo == null ? null : torneo.getNombre(),
                    t.getEstado(), t.getIniciadaEn(), t.getFinalizadaEn(),
                    muestras.getOrDefault(t.getId(), 0L));
        }).toList();
    }

    @Transactional(readOnly = true)
    public SeriesTransmisionResponse series(Long transmisionId, LocalDateTime desde, LocalDateTime hasta,
                                            String agrupacionSolicitada, String correo, boolean esAdmin) {
        AgrupacionMetrica agrupacion = AgrupacionMetrica.desde(agrupacionSolicitada);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("El rango de fechas no es valido");
        }
        TransmisionTwitch transmision = transmisionRepository.findById(transmisionId)
                .orElseThrow(() -> new ResourceNotFoundException("La transmisión no existe."));
        autorizar(transmision, correo, esAdmin);

        LocalDateTime inicio = desde == null ? SIN_COTA_INFERIOR : desde;
        LocalDateTime fin = hasta == null ? SIN_COTA_SUPERIOR : hasta;

        List<MetricaAudiencia> audiencia =
                audienciaRepository.buscarPorTransmisionYRango(transmisionId, inicio, fin);
        List<MetricaChat> chat =
                chatRepository.buscarPorTransmisionYRango(transmisionId, inicio, fin);
        List<AnalisisSentimiento> sentimiento =
                sentimientoRepository.buscarPorTransmisionYRango(transmisionId, inicio, fin);

        Map<ClaveSerie, List<Muestra>> crudas = new LinkedHashMap<>();
        crudas.put(ClaveSerie.ESPECTADORES, muestrasDe(audiencia,
                MetricaAudiencia::getFechaHora, m -> m.getEspectadores().doubleValue()));
        crudas.put(ClaveSerie.MENSAJES_POR_MINUTO, muestrasDe(chat,
                MetricaChat::getFechaHora, m -> m.getMensajesPorMinuto().doubleValue()));
        crudas.put(ClaveSerie.USUARIOS_ACTIVOS, muestrasDe(chat,
                MetricaChat::getFechaHora, m -> m.getUsuariosActivos().doubleValue()));
        crudas.put(ClaveSerie.SENTIMIENTO, muestrasDe(sentimiento,
                AnalisisSentimiento::getFechaHora,
                a -> a.getPuntaje().doubleValue()));

        List<SeriesTransmisionResponse.Serie> series = agrupacion == AgrupacionMetrica.HORA
                ? seriesPorHora(crudas)
                : seriesCrudas(crudas);

        Torneo torneo = torneoDe(transmision);
        return new SeriesTransmisionResponse(transmision.getId(), etiquetaDe(transmision, torneo),
                transmision.getEstado(), agrupacion.name(), desde, hasta,
                duracionMinutos(transmision), intervaloSegundos, origenDe(audiencia),
                resumenDe(crudas, sentimiento), series);
    }

    // --- series -------------------------------------------------------------

    private List<SeriesTransmisionResponse.Serie> seriesCrudas(Map<ClaveSerie, List<Muestra>> crudas) {
        return crudas.entrySet().stream()
                .map(e -> serie(e.getKey(), e.getValue(), e.getValue().stream()
                        .map(m -> new SeriesTransmisionResponse.Punto(m.instante(), m.valor()))
                        .toList()))
                .toList();
    }

    /**
     * Agrupa por hora y alinea: todas las series comparten los mismos buckets
     * (la unión de horas presentes en cualquiera), rellenando con valor nulo
     * donde una serie no tuvo muestras. Así el frontend grafica sin realinear.
     */
    private List<SeriesTransmisionResponse.Serie> seriesPorHora(Map<ClaveSerie, List<Muestra>> crudas) {
        Map<ClaveSerie, Map<LocalDateTime, Double>> promedios = new LinkedHashMap<>();
        TreeSet<LocalDateTime> buckets = new TreeSet<>();
        crudas.forEach((clave, muestras) -> {
            Map<LocalDateTime, Double> porHora = muestras.stream().collect(Collectors.groupingBy(
                    m -> m.instante().truncatedTo(ChronoUnit.HOURS), TreeMap::new,
                    Collectors.averagingDouble(Muestra::valor)));
            promedios.put(clave, porHora);
            buckets.addAll(porHora.keySet());
        });

        return crudas.entrySet().stream().map(e -> {
            Map<LocalDateTime, Double> porHora = promedios.get(e.getKey());
            List<SeriesTransmisionResponse.Punto> puntos = buckets.stream()
                    .map(h -> new SeriesTransmisionResponse.Punto(h, porHora.get(h)))
                    .toList();
            return serie(e.getKey(), e.getValue(), puntos);
        }).toList();
    }

    /**
     * Los agregados de la serie se calculan sobre las muestras crudas, no sobre
     * los buckets: el pico real de espectadores no debe depender de la agrupación
     * elegida para verlo.
     */
    private SeriesTransmisionResponse.Serie serie(ClaveSerie clave, List<Muestra> crudas,
                                                  List<SeriesTransmisionResponse.Punto> puntos) {
        Double promedio = crudas.isEmpty() ? null
                : crudas.stream().mapToDouble(Muestra::valor).average().orElse(0);
        Double pico = crudas.isEmpty() ? null
                : crudas.stream().mapToDouble(Muestra::valor).max().orElse(0);
        Double minimo = crudas.isEmpty() ? null
                : crudas.stream().mapToDouble(Muestra::valor).min().orElse(0);
        return new SeriesTransmisionResponse.Serie(clave, clave.etiqueta(), clave.unidad(),
                crudas.size(), promedio, pico, minimo, puntos);
    }

    private <T> List<Muestra> muestrasDe(List<T> filas, Function<T, LocalDateTime> instante,
                                         Function<T, Double> valor) {
        return filas.stream().map(f -> new Muestra(instante.apply(f), valor.apply(f))).toList();
    }

    private SeriesTransmisionResponse.Resumen resumenDe(
            Map<ClaveSerie, List<Muestra>> crudas,
            List<AnalisisSentimiento> sentimiento) {
        List<Muestra> espectadores = crudas.get(ClaveSerie.ESPECTADORES);
        List<Muestra> mensajes = crudas.get(ClaveSerie.MENSAJES_POR_MINUTO);
        List<Muestra> usuarios = crudas.get(ClaveSerie.USUARIOS_ACTIVOS);
        List<Muestra> puntajes = crudas.get(ClaveSerie.SENTIMIENTO);

        return new SeriesTransmisionResponse.Resumen(
                espectadores.size(),
                espectadores.isEmpty() ? null
                        : (int) Math.round(espectadores.stream().mapToDouble(Muestra::valor).max().orElse(0)),
                espectadores.isEmpty() ? null
                        : espectadores.stream().mapToDouble(Muestra::valor).average().orElse(0),
                mensajes.size(),
                mensajes.isEmpty() ? null : mensajes.stream().mapToDouble(Muestra::valor).average().orElse(0),
                usuarios.isEmpty() ? null
                        : (int) Math.round(usuarios.stream().mapToDouble(Muestra::valor).max().orElse(0)),
                puntajes.size(),
                puntajes.isEmpty() ? null : puntajes.stream().mapToDouble(Muestra::valor).average().orElse(0),
                clasificacionPredominante(sentimiento));
    }

    /** Moda de las clasificaciones; los empates se resuelven alfabéticamente para que sea determinista. */
    private String clasificacionPredominante(
            List<AnalisisSentimiento> sentimiento) {
        return sentimiento.stream()
                .collect(Collectors.groupingBy(
                        AnalisisSentimiento::getClasificacion,
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    /** RF-36 exige distinguir datos reales de simulados; el panel lo rotula. */
    private String origenDe(List<MetricaAudiencia> audiencia) {
        List<OrigenMetrica> distintos = audiencia.stream()
                .map(MetricaAudiencia::getOrigen).filter(Objects::nonNull).distinct().toList();
        if (distintos.isEmpty()) {
            return null;
        }
        return distintos.size() == 1 ? distintos.get(0).name() : "MIXTO";
    }

    private Long duracionMinutos(TransmisionTwitch transmision) {
        if (transmision.getIniciadaEn() == null) {
            return null;
        }
        LocalDateTime fin = transmision.getFinalizadaEn() == null
                ? LocalDateTime.now() : transmision.getFinalizadaEn();
        return Duration.between(transmision.getIniciadaEn(), fin).toMinutes();
    }

    // --- acceso -------------------------------------------------------------

    private void autorizar(TransmisionTwitch transmision, String correo, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        if (!puedeVer(transmision, usuarioActual(correo))) {
            throw new ForbiddenException("No tiene acceso a las metricas de esta transmision");
        }
    }

    /**
     * Mismo criterio que RF-32: el comisionado lo es de la liga de la temporada
     * del torneo, no del torneo directamente.
     *
     * <p>PATROCINADOR queda fuera por ahora: el vínculo usuario→patrocinador lo
     * agrega RF-44 (columna patrocinador.usuario_id), que todavía no está en
     * develop. Cuando entre, se suma acá la consulta a PatrocinioRepository.
     */
    private boolean puedeVer(TransmisionTwitch transmision, Usuario usuario) {
        Torneo torneo = torneoDe(transmision);
        if (torneo == null || torneo.getTemporada() == null
                || torneo.getTemporada().getLiga() == null
                || torneo.getTemporada().getLiga().getComisionado() == null) {
            return false;
        }
        return torneo.getTemporada().getLiga().getComisionado().getId().equals(usuario.getId());
    }

    private Usuario usuarioActual(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ForbiddenException("No se pudo identificar al usuario"));
    }

    /** La transmisión puede colgar del torneo o de una partida; Partida.torneo nunca es nulo. */
    private Torneo torneoDe(TransmisionTwitch transmision) {
        if (transmision.getTorneo() != null) {
            return transmision.getTorneo();
        }
        return transmision.getPartida() == null ? null : transmision.getPartida().getTorneo();
    }

    private String etiquetaDe(TransmisionTwitch transmision, Torneo torneo) {
        List<String> partes = new ArrayList<>();
        if (torneo != null) {
            partes.add(torneo.getNombre());
        } else if (transmision.getLoginCanal() != null) {
            partes.add(transmision.getLoginCanal());
        } else {
            partes.add("Transmisión #" + transmision.getId());
        }
        if (transmision.getIniciadaEn() != null) {
            partes.add(ETIQUETA_FECHA.format(transmision.getIniciadaEn()));
        }
        return String.join(" — ", partes);
    }

    private record Muestra(LocalDateTime instante, double valor) {
    }
}
