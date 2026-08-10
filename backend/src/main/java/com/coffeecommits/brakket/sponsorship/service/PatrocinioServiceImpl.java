package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
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

import java.util.List;
import java.util.Set;

@Service
public class PatrocinioServiceImpl implements PatrocinioService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final Set<String> NIVELES_VALIDOS = Set.of("ORO", "PLATA", "BRONCE");

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

        if (!NIVELES_VALIDOS.contains(request.nivel())) {
            throw new BusinessException(
                    "El nivel debe ser uno de: %s".formatted(String.join(", ", NIVELES_VALIDOS)));
        }

        validarAlcanceUnico(request);

        if (request.fechaInicio().isAfter(request.fechaFin())) {
            throw new BusinessException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        Liga liga = null;
        Temporada temporada = null;
        Torneo torneo = null;

        if (request.ligaId() != null) {
            liga = ligaRepository.findById(request.ligaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Liga", request.ligaId()));
            // Liga no tiene fechas propias en el esquema actual: no hay calendario contra qué validar.

        } else if (request.temporadaId() != null) {
            temporada = temporadaRepository.findById(request.temporadaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Temporada", request.temporadaId()));
            validarFechasDentroDeRango(request, temporada.getFechaInicio(), temporada.getFechaFin());

        } else {
            torneo = torneoRepository.findById(request.torneoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Torneo", request.torneoId()));
            // torneo.getFechaFin() puede ser null (torneo sin calendario cerrado aun);
            // en ese caso solo se valida el limite inferior.
            validarFechasDentroDeRango(request, torneo.getFechaInicio().toLocalDate(),
                    torneo.getFechaFin() != null ? torneo.getFechaFin().toLocalDate() : null);
        }

        // El recurso escaso es (competencia, nivel), no la competencia entera:
        // dos patrocinios de niveles distintos (ej. ORO y PLATA) pueden coexistir
        // en la misma competencia y periodo. Lo que no puede repetirse es el
        // mismo nivel dos veces para la misma competencia en fechas solapadas.
        boolean solapa = patrocinioRepository.existeSolapamiento(
                request.ligaId(), request.temporadaId(), request.torneoId(),
                request.nivel(), request.fechaInicio(), request.fechaFin());

        if (solapa) {
            throw new BusinessException(
                    "Ya existe un patrocinio de nivel %s activo para esta competencia en el período seleccionado"
                            .formatted(request.nivel()));
        }

        Patrocinio patrocinio = Patrocinio.builder()
                .patrocinador(patrocinador)
                .liga(liga)
                .temporada(temporada)
                .torneo(torneo)
                .nivel(request.nivel())
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

    private void validarAlcanceUnico(CrearPatrocinioRequest request) {
        int alcancesLlenos = 0;
        if (request.ligaId() != null) alcancesLlenos++;
        if (request.temporadaId() != null) alcancesLlenos++;
        if (request.torneoId() != null) alcancesLlenos++;

        if (alcancesLlenos != 1) {
            throw new BusinessException(
                    "Debe especificar exactamente una competencia: liga, temporada o torneo");
        }
    }

    private void validarFechasDentroDeRango(CrearPatrocinioRequest request,
                                            java.time.LocalDate inicioCompetencia,
                                            java.time.LocalDate finCompetencia) {
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