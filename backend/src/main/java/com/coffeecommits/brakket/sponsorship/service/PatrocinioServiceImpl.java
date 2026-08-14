package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinioRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PatrocinioServiceImpl implements PatrocinioService {

    private static final String ESTADO_ACTIVO = "ACTIVO";

    private static final java.util.Set<String> ESTADOS_CERRADOS_TEMPORADA =
            java.util.Set.of("FINALIZADA", "CANCELADA");

    private final PatrocinioRepository patrocinioRepository;
    private final PatrocinadorRepository patrocinadorRepository;
    private final LigaRepository ligaRepository;
    private final TemporadaRepository temporadaRepository;
    private final TorneoRepository torneoRepository;

    public PatrocinioServiceImpl(PatrocinioRepository patrocinioRepository,
                                 PatrocinadorRepository patrocinadorRepository,
                                 LigaRepository ligaRepository,
                                 TemporadaRepository temporadaRepository,
                                 TorneoRepository torneoRepository) {
        this.patrocinioRepository = patrocinioRepository;
        this.patrocinadorRepository = patrocinadorRepository;
        this.ligaRepository = ligaRepository;
        this.temporadaRepository = temporadaRepository;
        this.torneoRepository = torneoRepository;
    }

    @Override
    @Transactional
    public PatrocinioResponse crear(CrearPatrocinioRequest request) {

        Patrocinador patrocinador = patrocinadorRepository.findById(request.patrocinadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinador", request.patrocinadorId()));

        if (!ESTADO_ACTIVO.equals(patrocinador.getEstado())) {
            throw new BusinessException("El patrocinador debe estar activo para asociarse a una competencia");
        }

        validarAlcanceUnico(request);

        if (request.fechaInicio().isAfter(request.fechaFin())) {
            throw new BusinessException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        Liga liga = null;
        Torneo torneo = null;

        if (request.ligaId() != null) {
            liga = ligaRepository.findById(request.ligaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Liga", request.ligaId()));
            if (!Boolean.TRUE.equals(liga.getActivo())) {
                throw new BusinessException("No se puede asociar un patrocinio a una liga inactiva");
            }

        } else {
            torneo = torneoRepository.findById(request.torneoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Torneo", request.torneoId()));
            if (TorneoRepository.ESTADOS_CERRADOS.contains(torneo.getEstado())) {
                throw new BusinessException(
                        "No se puede asociar un patrocinio a un torneo finalizado o cancelado");
            }
            validarFechasDentroDeRango(request, torneo.getFechaInicio().toLocalDate(),
                    torneo.getFechaFin() != null ? torneo.getFechaFin().toLocalDate() : null);
        }

        boolean solapa = patrocinioRepository.existeSolapamiento(
                request.ligaId(), request.torneoId(), request.fechaInicio(), request.fechaFin());

        if (solapa) {
            throw new BusinessException(
                    "Ya existe un patrocinio activo para esta competencia en el período seleccionado");
        }

        Patrocinio patrocinio = Patrocinio.builder()
                .patrocinador(patrocinador)
                .liga(liga)
                .temporada(null)
                .torneo(torneo)
                .condiciones(request.condiciones())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .estado(ESTADO_ACTIVO)
                .build();

        patrocinio = patrocinioRepository.save(patrocinio);
        return PatrocinioResponse.fromEntity(patrocinio);
    }

    @Override
    @Transactional(readOnly = true)
    public PatrocinioResponse obtenerPorId(Long id) {
        Patrocinio patrocinio = patrocinioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinio", id));
        return PatrocinioResponse.fromEntity(patrocinio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatrocinioResponse> listarPorTorneo(Long torneoId) {
        return patrocinioRepository.findByTorneoId(torneoId).stream()
                .map(PatrocinioResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatrocinioResponse> listarPorLiga(Long ligaId) {
        return patrocinioRepository.findByLigaId(ligaId).stream()
                .map(PatrocinioResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatrocinioResponse> listarPorTemporada(Long temporadaId) {
        return patrocinioRepository.findByTemporadaId(temporadaId).stream()
                .map(PatrocinioResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatrocinioResponse> listarTodos() {
        return patrocinioRepository.findAll().stream()
                .map(PatrocinioResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatrocinioResponse> resolverVigentePorTorneo(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo", torneoId));

        Optional<Patrocinio> propio = patrocinioRepository.findByTorneoId(torneoId).stream()
                .filter(this::estaVigenteHoy)
                .findFirst();
        if (propio.isPresent()) {
            return propio.map(PatrocinioResponse::fromEntity);
        }

        if (torneo.getTemporada() != null && torneo.getTemporada().getLiga() != null) {
            Long ligaId = torneo.getTemporada().getLiga().getId();
            return patrocinioRepository.findByLigaId(ligaId).stream()
                    .filter(this::estaVigenteHoy)
                    .findFirst()
                    .map(PatrocinioResponse::fromEntity);
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Patrocinio patrocinio = patrocinioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patrocinio", id));
        patrocinioRepository.delete(patrocinio);
    }

    private boolean estaVigenteHoy(Patrocinio p) {
        LocalDate hoy = LocalDate.now();
        return ESTADO_ACTIVO.equals(p.getEstado())
                && !p.getFechaInicio().isAfter(hoy)
                && !p.getFechaFin().isBefore(hoy);
    }

    private void validarAlcanceUnico(CrearPatrocinioRequest request) {
        if (request.temporadaId() != null) {
            throw new BusinessException(
                    "Los patrocinios ya no se asocian directamente a una temporada: usa Liga "
                            + "(patrocinio principal) o Torneo (uno específico para ese evento).");
        }
        int alcancesLlenos = 0;
        if (request.ligaId() != null) alcancesLlenos++;
        if (request.torneoId() != null) alcancesLlenos++;

        if (alcancesLlenos != 1) {
            throw new BusinessException("Debe especificar exactamente una competencia: liga o torneo");
        }
    }

    private void validarFechasDentroDeRango(CrearPatrocinioRequest request,
                                            LocalDate inicioCompetencia,
                                            LocalDate finCompetencia) {
        if (request.fechaInicio().isBefore(inicioCompetencia)) {
            throw new BusinessException(
                    "La fecha de inicio del patrocinio no puede ser anterior al inicio de la competencia");
        }
        if (finCompetencia != null && request.fechaFin().isAfter(finCompetencia)) {
            throw new BusinessException(
                    "La fecha de fin del patrocinio no puede ser posterior al fin de la competencia");
        }
    }
}