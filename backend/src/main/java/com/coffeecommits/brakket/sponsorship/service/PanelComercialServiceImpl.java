package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.service.AuthService;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.sponsorship.dto.MetricasPatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.dto.PanelComercialResponse;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.EspacioPublicitarioRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
                .orElseThrow(() -> new ForbiddenException(
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
                        p.getEstado(),
                        p.getFechaFin().isBefore(LocalDate.now()),
                        p.getLiga() != null ? p.getLiga().getId() : null,
                        p.getTemporada() != null ? p.getTemporada().getId() : null,
                        p.getTorneo() != null ? p.getTorneo().getId() : null,
                        p.getFechaInicio(),
                        p.getFechaFin(),
                        (int) espacioRepository.countByPatrocinioId(p.getId())
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
            throw new ForbiddenException("No tenés permiso para consultar métricas de este patrocinio.");
        }

        if (patrocinio.getTorneo() == null) {
            return new MetricasPatrocinioResponse(patrocinioId, null, null, null, null, null, true);
        }

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

    private Optional<TransmisionTwitch> seleccionarTransmisionRelevante(List<TransmisionTwitch> transmisiones) {
        return transmisiones.stream()
                .filter(t -> t.getFinalizadaEn() == null)
                .findFirst()
                .or(() -> transmisiones.stream().findFirst());
    }

    private String calcularSentimientoPredominante(Long transmisionId) {
        List<AnalisisSentimientoRepository.ConteoClasificacion> conteos =
                sentimientoRepository.contarPorClasificacionDeTransmision(transmisionId);

        if (conteos.isEmpty()) {
            return null;
        }

        return conteos.stream()
                .max(Comparator.comparingLong(AnalisisSentimientoRepository.ConteoClasificacion::getCantidad))
                .map(AnalisisSentimientoRepository.ConteoClasificacion::getClasificacion)
                .orElse(null);
    }
}