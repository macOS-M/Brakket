package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.service.AuthService;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.sponsorship.dto.MetricasPatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.dto.PanelComercialResponse;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.EspacioPublicitarioRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PanelComercialServiceImpl implements PanelComercialService {

    private final AuthService authService;
    private final PatrocinadorRepository patrocinadorRepository;
    private final PatrocinioRepository patrocinioRepository;
    private final EspacioPublicitarioRepository espacioRepository;
    private final TransmisionTwitchRepository transmisionRepository;
    private final MetricaAudienciaRepository metricaAudienciaRepository;
    private final MetricaChatRepository metricaChatRepository;
    private final AnalisisSentimientoRepository sentimientoRepository;

    public PanelComercialServiceImpl(AuthService authService,
                                     PatrocinadorRepository patrocinadorRepository,
                                     PatrocinioRepository patrocinioRepository,
                                     EspacioPublicitarioRepository espacioRepository,
                                     TransmisionTwitchRepository transmisionRepository,
                                     MetricaAudienciaRepository metricaAudienciaRepository,
                                     MetricaChatRepository metricaChatRepository,
                                     AnalisisSentimientoRepository sentimientoRepository) {
        this.authService = authService;
        this.patrocinadorRepository = patrocinadorRepository;
        this.patrocinioRepository = patrocinioRepository;
        this.espacioRepository = espacioRepository;
        this.transmisionRepository = transmisionRepository;
        this.metricaAudienciaRepository = metricaAudienciaRepository;
        this.metricaChatRepository = metricaChatRepository;
        this.sentimientoRepository = sentimientoRepository;
    }

    private Patrocinador resolverPatrocinadorAutenticado(Authentication authentication) {
        UsuarioResponse usuario = authService.getCurrentUser(authentication.getName());
        return patrocinadorRepository.findByUsuarioId(usuario.id())
                .orElseThrow(() -> new BusinessException(
                        "Tu cuenta no está vinculada a ningún perfil de patrocinador."));
    }

    @Override
    @Transactional(readOnly = true)
    public PanelComercialResponse obtenerResumen(Authentication authentication) {
        Patrocinador patrocinador = resolverPatrocinadorAutenticado(authentication);

        List<Patrocinio> patrocinios = patrocinioRepository.findByPatrocinadorId(patrocinador.getId());

        List<PanelComercialResponse.PatrocinioResumen> resumenes = patrocinios.stream()
                .map(p -> new PanelComercialResponse.PatrocinioResumen(
                        p.getId(),
                        p.getNivel(),
                        p.getEstado(),
                        p.getFechaFin().isBefore(LocalDate.now()),
                        p.getLiga() != null ? p.getLiga().getId() : null,
                        p.getTemporada() != null ? p.getTemporada().getId() : null,
                        p.getTorneo() != null ? p.getTorneo().getId() : null,
                        p.getFechaInicio(),
                        p.getFechaFin(),
                        espacioRepository.findByPatrocinioId(p.getId()).size()
                ))
                .toList();

        return new PanelComercialResponse(patrocinador.getId(), patrocinador.getNombre(), resumenes);
    }

    @Override
    @Transactional(readOnly = true)
    public MetricasPatrocinioResponse obtenerMetricas(Authentication authentication, Long patrocinioId) {
        Patrocinador patrocinador = resolverPatrocinadorAutenticado(authentication);

        Patrocinio patrocinio = patrocinioRepository.findById(patrocinioId)
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinio", patrocinioId));

        if (!patrocinio.getPatrocinador().getId().equals(patrocinador.getId())) {
            throw new BusinessException("No tenés permiso para consultar métricas de este patrocinio.");
        }

        if (patrocinio.getTorneo() == null) {
            // Opcion A (decision de alcance): solo se calculan metricas reales
            // cuando el patrocinio tiene torneo directo, dado que transmision_twitch
            // solo se relaciona con torneo_id, no con liga_id ni temporada_id.
            return new MetricasPatrocinioResponse(patrocinioId, null, null, null, null, null, true);
        }

        // RF-44 review: un torneo puede tener varias transmisiones (varias
        // jornadas), asi que findByTorneoId no puede devolver Optional/uno solo
        // sin arriesgar IncorrectResultSizeDataAccessException. Se trae la lista
        // completa y se elige la mas relevante: la que sigue en vivo, o si
        // ninguna esta en vivo, la mas reciente.
        List<TransmisionTwitch> transmisiones =
                transmisionRepository.findByTorneoIdOrderByIniciadaEnDesc(patrocinio.getTorneo().getId());

        Optional<TransmisionTwitch> transmisionRelevante = seleccionarTransmisionRelevante(transmisiones);

        if (transmisionRelevante.isEmpty()) {
            return new MetricasPatrocinioResponse(patrocinioId, null, null, null, null, null, true);
        }

        Long transmisionId = transmisionRelevante.get().getId();

        MetricaAudienciaRepository.ResumenAudiencia audiencia =
                metricaAudienciaRepository.resumenPorTransmision(transmisionId);
        MetricaChatRepository.ResumenChat chat =
                metricaChatRepository.resumenPorTransmision(transmisionId);

        String sentimientoPredominante = calcularSentimientoPredominante(transmisionId);

        return new MetricasPatrocinioResponse(
                patrocinioId,
                transmisionId,
                audiencia != null && audiencia.getPromedio() != null
                        ? audiencia.getPromedio().intValue() : null,
                audiencia != null ? audiencia.getPico() : null,
                chat != null ? chat.getMensajesPorMinutoPromedio() : null,
                sentimientoPredominante,
                sentimientoPredominante == null
        );
    }

    // Prioriza la transmision actualmente en vivo; si ninguna esta en vivo,
    // toma la mas reciente (la lista ya llega ordenada por iniciadaEn desc).
    private Optional<TransmisionTwitch> seleccionarTransmisionRelevante(List<TransmisionTwitch> transmisiones) {
        return transmisiones.stream()
                .filter(t -> t.getFinalizadaEn() == null)
                .findFirst()
                .or(() -> transmisiones.stream().findFirst());
    }

    private String calcularSentimientoPredominante(Long transmisionId) {
        List<MetricaChat> muestrasChat = metricaChatRepository.findByTransmisionTwitchId(transmisionId);

        List<AnalisisSentimiento> clasificaciones = muestrasChat.stream()
                .flatMap(m -> sentimientoRepository.findByMetricaChatId(m.getId()).stream())
                .toList();

        if (clasificaciones.isEmpty()) {
            return null; // sin analisis todavia -> "pendiente" (RF-40, comportamiento valido)
        }

        Map<String, Long> conteoPorClasificacion = clasificaciones.stream()
                .collect(Collectors.groupingBy(AnalisisSentimiento::getClasificacion, Collectors.counting()));

        return conteoPorClasificacion.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}