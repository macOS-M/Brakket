package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.tournament.dto.CalendarioEventoResponse;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CalendarioServiceImpl implements CalendarioService {

    private final TorneoRepository torneoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;

    public CalendarioServiceImpl(TorneoRepository torneoRepository,
                                 UsuarioRepository usuarioRepository,
                                 InscripcionRepository inscripcionRepository) {
        this.torneoRepository = torneoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarioEventoResponse> consultar(LocalDateTime desde, LocalDateTime hasta,
                                                    Long juegoId, Long ligaId, Long torneoId,
                                                    EstadoTorneo estado, Long equipoId,
                                                    String correoSolicitante, boolean esAdmin) {

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("El rango de fechas no es valido");
        }

        Usuario usuario = usuarioRepository.findByCorreo(correoSolicitante)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correoSolicitante));

        Set<Long> torneoIdsDelEquipo = equipoId == null ? null
                : inscripcionRepository.findByEquipoId(equipoId).stream()
                .map(i -> i.getTorneo().getId())
                .collect(Collectors.toSet());

        return torneoRepository.buscarVisiblesParaCalendario(usuario.getId(), esAdmin).stream()
                .filter(t -> desde == null || !t.getFechaInicio().isBefore(desde))
                .filter(t -> hasta == null || !t.getFechaInicio().isAfter(hasta))
                .filter(t -> juegoId == null || t.getJuego().getId().equals(juegoId))
                .filter(t -> ligaId == null || coincideLiga(t, ligaId))
                .filter(t -> torneoId == null || t.getId().equals(torneoId))
                .filter(t -> estado == null || t.getEstado() == estado)
                .filter(t -> torneoIdsDelEquipo == null || torneoIdsDelEquipo.contains(t.getId()))
                .map(CalendarioEventoResponse::fromEntity)
                .toList();
    }

    private boolean coincideLiga(Torneo t, Long ligaId) {
        return t.getTemporada() != null
                && t.getTemporada().getLiga() != null
                && t.getTemporada().getLiga().getId().equals(ligaId);
    }
}