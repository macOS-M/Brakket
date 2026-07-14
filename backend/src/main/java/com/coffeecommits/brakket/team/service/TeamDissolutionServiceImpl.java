package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TeamDissolutionServiceImpl implements TeamDissolutionService {

    static final String ESTADO_ACTIVO = "ACTIVO";
    static final String ESTADO_DISUELTO = "DISUELTO";

    private final EquipoRepository equipoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PartidaRepository partidaRepository;

    public TeamDissolutionServiceImpl(EquipoRepository equipoRepository,
                                      MiembroEquipoRepository miembroEquipoRepository,
                                      UsuarioRepository usuarioRepository,
                                      InscripcionRepository inscripcionRepository,
                                      PartidaRepository partidaRepository) {
        this.equipoRepository = equipoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.partidaRepository = partidaRepository;
    }

    @Override
    @Transactional
    public EquipoResponse disolver(Long equipoId, DisolverEquipoRequest request, String solicitanteCorreo) {
        Usuario solicitante = usuarioRepository.findByCorreo(solicitanteCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", solicitanteCorreo));

        MiembroEquipo miembroSolicitante = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, solicitante.getId())
                .orElseThrow(() -> new BusinessException("No tiene permisos sobre este equipo"));

        if (!"CAPITAN".equals(miembroSolicitante.getRol()) || !ESTADO_ACTIVO.equals(miembroSolicitante.getEstado())) {
            throw new BusinessException("Solo el capitán activo del equipo puede disolverlo");
        }

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        if (!ESTADO_ACTIVO.equals(equipo.getEstado())) {
            throw new BusinessException("El equipo '%s' ya está disuelto".formatted(equipo.getNombre()));
        }

        if (inscripcionRepository.existsInscripcionActivaPorEquipo(equipoId)) {
            throw new BusinessException(
                    "No se puede disolver '%s': tiene inscripciones activas en torneos. Resuélvalas o solicite autorización administrativa."
                            .formatted(equipo.getNombre()));
        }

        if (partidaRepository.existsPartidaPendientePorEquipo(equipoId)) {
            throw new BusinessException(
                    "No se puede disolver '%s': tiene partidas pendientes por resolver."
                            .formatted(equipo.getNombre()));
        }

        equipo.setEstado(ESTADO_DISUELTO);
        equipo.setFechaDisolucion(LocalDateTime.now());
        equipo.setDisueltoPor(solicitante);
        equipo.setMotivoDisolucion(normalizar(request.motivo()));
        equipoRepository.save(equipo);

        // TODO RF-45 (EPIC-12): notificar la disolución a los integrantes cuando
        // exista el módulo de notificaciones.

        return EquipoResponse.fromEntity(equipo);
    }

    private String normalizar(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
