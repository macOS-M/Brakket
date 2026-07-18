package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.dto.PerfilCompetitivoRequest;
import com.coffeecommits.brakket.game.dto.PerfilCompetitivoResponse;
import com.coffeecommits.brakket.game.model.EstadisticaJuego;
import com.coffeecommits.brakket.game.model.FormatoCompetitivo;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.model.ModalidadCompetitiva;
import com.coffeecommits.brakket.game.model.PerfilCompetitivoJuego;
import com.coffeecommits.brakket.game.repository.EstadisticaJuegoRepository;
import com.coffeecommits.brakket.game.repository.FormatoCompetitivoRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.game.repository.PerfilCompetitivoRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CompetitiveProfileService {

    private final PerfilCompetitivoRepository perfilRepository;
    private final JuegoRepository juegoRepository;
    private final FormatoCompetitivoRepository formatoRepository;
    private final EstadisticaJuegoRepository estadisticaRepository;
    private final TorneoRepository torneoRepository;

    public CompetitiveProfileService(PerfilCompetitivoRepository perfilRepository,
                                     JuegoRepository juegoRepository,
                                     FormatoCompetitivoRepository formatoRepository,
                                     EstadisticaJuegoRepository estadisticaRepository,
                                     TorneoRepository torneoRepository) {
        this.perfilRepository = perfilRepository;
        this.juegoRepository = juegoRepository;
        this.formatoRepository = formatoRepository;
        this.estadisticaRepository = estadisticaRepository;
        this.torneoRepository = torneoRepository;
    }

    @Transactional
    public PerfilCompetitivoResponse crear(PerfilCompetitivoRequest request) {
        validarCoherencia(request);
        Juego juego = obtenerJuegoActivo(request.juegoId());
        if (perfilRepository.findByJuegoId(juego.getId()).isPresent()) {
            throw new BusinessException("El juego ya tiene un perfil competitivo; utilice la actualización");
        }

        List<FormatoCompetitivo> formatos = obtenerFormatosActivos(request.formatosIds());
        List<EstadisticaJuego> estadisticas = obtenerEstadisticasActivas(request.estadisticasIds());
        validarEstadisticasObligatorias(estadisticas);

        PerfilCompetitivoJuego perfil = PerfilCompetitivoJuego.builder()
                .juego(juego)
                .modalidad(request.modalidad())
                .plantillaMinima(request.plantillaMinima())
                .plantillaMaxima(request.plantillaMaxima())
                .activo(true)
                .formatosCompatibles(formatos)
                .estadisticas(estadisticas)
                .build();

        return mapear(perfilRepository.save(perfil), "Perfil competitivo creado correctamente");
    }

    @Transactional
    public PerfilCompetitivoResponse actualizar(Long id, PerfilCompetitivoRequest request) {
        validarCoherencia(request);
        PerfilCompetitivoJuego perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil competitivo", id));
        Juego juego = obtenerJuegoActivo(request.juegoId());

        perfilRepository.findByJuegoId(juego.getId())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessException("El juego ya tiene otro perfil competitivo");
                });

        List<FormatoCompetitivo> formatos = obtenerFormatosActivos(request.formatosIds());
        List<EstadisticaJuego> estadisticas = obtenerEstadisticasActivas(request.estadisticasIds());
        validarEstadisticasObligatorias(estadisticas);

        if (esCambioCritico(perfil, request) && torneoRepository.existsActivoByJuegoId(perfil.getJuego().getId())) {
            throw new BusinessException(
                    "No se pueden cambiar modalidad, plantilla, formatos o juego mientras existan torneos activos");
        }

        perfil.setJuego(juego);
        perfil.setModalidad(request.modalidad());
        perfil.setPlantillaMinima(request.plantillaMinima());
        perfil.setPlantillaMaxima(request.plantillaMaxima());
        perfil.setFormatosCompatibles(formatos);
        perfil.setEstadisticas(estadisticas);

        return mapear(perfilRepository.save(perfil), "Perfil competitivo actualizado correctamente");
    }

    public PerfilCompetitivoResponse obtener(Long id) {
        return mapear(perfilRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil competitivo", id)), null);
    }

    public PerfilCompetitivoResponse obtenerPorJuego(Long juegoId) {
        return mapear(perfilRepository.findByJuegoId(juegoId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil competitivo del juego", juegoId)), null);
    }

    private void validarCoherencia(PerfilCompetitivoRequest request) {
        if (request.plantillaMinima() > request.plantillaMaxima()) {
            throw new IllegalArgumentException("El tamaño mínimo no puede ser mayor al máximo");
        }
        if (request.modalidad() == ModalidadCompetitiva.INDIVIDUAL
                && (request.plantillaMinima() != 1 || request.plantillaMaxima() != 1)) {
            throw new IllegalArgumentException("La modalidad individual requiere plantilla mínima y máxima de 1");
        }
        validarIdsSinDuplicados(request.formatosIds(), "formatos");
        validarIdsSinDuplicados(request.estadisticasIds(), "estadísticas");
    }

    private Juego obtenerJuegoActivo(Long juegoId) {
        Juego juego = juegoRepository.findById(juegoId)
                .orElseThrow(() -> new ResourceNotFoundException("Juego", juegoId));
        if (!Boolean.TRUE.equals(juego.getActivo())) {
            throw new BusinessException("El juego seleccionado no está activo");
        }
        return juego;
    }

    private List<FormatoCompetitivo> obtenerFormatosActivos(List<Long> ids) {
        List<FormatoCompetitivo> formatos = formatoRepository.findAllById(ids);
        if (formatos.size() != ids.size()) {
            throw new IllegalArgumentException("Uno o más formatos no pertenecen al catálogo");
        }
        if (formatos.stream().anyMatch(f -> !Boolean.TRUE.equals(f.getActivo()))) {
            throw new BusinessException("Todos los formatos seleccionados deben estar activos");
        }
        return formatos;
    }

    private List<EstadisticaJuego> obtenerEstadisticasActivas(List<Long> ids) {
        List<EstadisticaJuego> estadisticas = estadisticaRepository.findAllById(ids);
        if (estadisticas.size() != ids.size()) {
            throw new IllegalArgumentException("Una o más estadísticas no pertenecen al catálogo");
        }
        if (estadisticas.stream().anyMatch(e -> !Boolean.TRUE.equals(e.getActiva()))) {
            throw new BusinessException("Todas las estadísticas seleccionadas deben estar activas");
        }
        return estadisticas;
    }

    private void validarEstadisticasObligatorias(List<EstadisticaJuego> seleccionadas) {
        Set<Long> ids = seleccionadas.stream().map(EstadisticaJuego::getId).collect(java.util.stream.Collectors.toSet());
        List<String> faltantes = estadisticaRepository.findByObligatoriaTrueAndActivaTrue().stream()
                .filter(e -> !ids.contains(e.getId()))
                .map(EstadisticaJuego::getNombre)
                .toList();
        if (!faltantes.isEmpty()) {
            throw new BusinessException("Faltan estadísticas obligatorias: " + String.join(", ", faltantes));
        }
    }

    private boolean esCambioCritico(PerfilCompetitivoJuego perfil, PerfilCompetitivoRequest request) {
        Set<Long> actuales = perfil.getFormatosCompatibles().stream()
                .map(FormatoCompetitivo::getId).collect(java.util.stream.Collectors.toSet());
        return !perfil.getJuego().getId().equals(request.juegoId())
                || perfil.getModalidad() != request.modalidad()
                || !Objects.equals(perfil.getPlantillaMinima(), request.plantillaMinima())
                || !Objects.equals(perfil.getPlantillaMaxima(), request.plantillaMaxima())
                || !actuales.equals(new HashSet<>(request.formatosIds()));
    }

    private void validarIdsSinDuplicados(List<Long> ids, String campo) {
        if (ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("El campo " + campo + " contiene identificadores nulos o duplicados");
        }
    }

    private PerfilCompetitivoResponse mapear(PerfilCompetitivoJuego perfil, String mensaje) {
        return new PerfilCompetitivoResponse(
                perfil.getId(), perfil.getJuego().getId(), perfil.getJuego().getNombre(),
                perfil.getModalidad().name(), perfil.getPlantillaMinima(), perfil.getPlantillaMaxima(),
                perfil.getFormatosCompatibles().stream().map(FormatoCompetitivo::getNombre).toList(),
                perfil.getEstadisticas().stream().map(EstadisticaJuego::getNombre).toList(),
                perfil.getFormatosCompatibles().stream().map(FormatoCompetitivo::getId).toList(),
                perfil.getEstadisticas().stream().map(EstadisticaJuego::getId).toList(),
                perfil.getActivo(), mensaje);
    }
}
