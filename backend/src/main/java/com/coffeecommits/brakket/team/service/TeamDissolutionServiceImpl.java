package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.admin.model.LogAuditoria;
import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.transfer.repository.HistorialTransferenciaRepository;
import com.coffeecommits.brakket.transfer.repository.SolicitudTransferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TeamDissolutionServiceImpl implements TeamDissolutionService {

    static final String ESTADO_ACTIVO = "ACTIVO";
    static final String ESTADO_DISUELTO = "DISUELTO";
    static final String ESTADO_BLOQUEADO = "BLOQUEADO";

    private final EquipoRepository equipoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PartidaRepository partidaRepository;
    private final TorneoRepository torneoRepository;
    private final HistorialTransferenciaRepository historialTransferenciaRepository;
    private final SolicitudTransferenciaRepository solicitudTransferenciaRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;

    public TeamDissolutionServiceImpl(EquipoRepository equipoRepository,
                                      MiembroEquipoRepository miembroEquipoRepository,
                                      UsuarioRepository usuarioRepository,
                                      InscripcionRepository inscripcionRepository,
                                      PartidaRepository partidaRepository,
                                      TorneoRepository torneoRepository,
                                      HistorialTransferenciaRepository historialTransferenciaRepository,
                                      SolicitudTransferenciaRepository solicitudTransferenciaRepository,
                                      LogAuditoriaRepository logAuditoriaRepository) {
        this.equipoRepository = equipoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.partidaRepository = partidaRepository;
        this.torneoRepository = torneoRepository;
        this.historialTransferenciaRepository = historialTransferenciaRepository;
        this.solicitudTransferenciaRepository = solicitudTransferenciaRepository;
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    @Override
    @Transactional
    public EquipoResponse disolver(Long equipoId, DisolverEquipoRequest request,
                                   String solicitanteCorreo, boolean esAdmin) {
        Usuario solicitante = buscarUsuario(solicitanteCorreo);
        exigirCapitanOAdmin(equipoId, solicitante, esAdmin, "disolverlo");

        Equipo equipo = buscarEquipo(equipoId);

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

    @Override
    @Transactional
    public EquipoResponse reactivar(Long equipoId, String solicitanteCorreo, boolean esAdmin) {
        Usuario solicitante = buscarUsuario(solicitanteCorreo);
        exigirCapitanOAdmin(equipoId, solicitante, esAdmin, "reactivarlo");

        Equipo equipo = buscarEquipo(equipoId);

        if (ESTADO_BLOQUEADO.equals(equipo.getEstado())) {
            throw new BusinessException(
                    "El equipo está bloqueado por una revisión administrativa; no se reactiva por esta vía.");
        }
        if (!ESTADO_DISUELTO.equals(equipo.getEstado())) {
            throw new BusinessException("El equipo '%s' ya está activo".formatted(equipo.getNombre()));
        }

        equipo.setEstado(ESTADO_ACTIVO);
        equipo.setFechaDisolucion(null);
        equipo.setDisueltoPor(null);
        equipo.setMotivoDisolucion(null);
        equipoRepository.save(equipo);

        auditar(solicitante, "EQUIPO_REACTIVADO", equipoId);
        return EquipoResponse.fromEntity(equipo);
    }

    @Override
    @Transactional
    public void eliminar(Long equipoId, String solicitanteCorreo, boolean esAdmin) {
        Usuario solicitante = buscarUsuario(solicitanteCorreo);
        exigirCapitanOAdmin(equipoId, solicitante, esAdmin, "eliminarlo");

        Equipo equipo = buscarEquipo(equipoId);

        // Dos pasos para el capitán (disolver → eliminar); el ADMIN modera
        // directo. El historial competitivo siempre frena el borrado físico:
        // se conserva disolviendo, no borrando.
        if (!esAdmin && !ESTADO_DISUELTO.equals(equipo.getEstado())) {
            throw new BusinessException(
                    "Disolvé el equipo antes de eliminarlo definitivamente.");
        }
        if (tieneHistorialCompetitivo(equipoId)) {
            throw new BusinessException(
                    "No se puede eliminar '%s': tiene historial competitivo (torneos, partidas o transferencias). Ese historial se conserva con el equipo disuelto."
                            .formatted(equipo.getNombre()));
        }

        // Miembros, invitaciones, solicitudes de unión, historial de roles y
        // redes sociales caen por CASCADE en la base.
        equipoRepository.delete(equipo);
        auditar(solicitante, "EQUIPO_ELIMINADO", equipoId);
    }

    // ---------- helpers ----------

    private boolean tieneHistorialCompetitivo(Long equipoId) {
        return inscripcionRepository.existsByEquipoId(equipoId)
                || partidaRepository.existsByEquipoAId(equipoId)
                || partidaRepository.existsByEquipoBId(equipoId)
                || partidaRepository.existsByGanadorId(equipoId)
                || torneoRepository.existsByCampeonId(equipoId)
                || historialTransferenciaRepository.existsByEquipoOrigenIdOrEquipoDestinoId(equipoId, equipoId)
                || solicitudTransferenciaRepository.existsByEquipoOrigenIdOrEquipoDestinoId(equipoId, equipoId);
    }

    /** Capitán activo del equipo, o ADMIN de la plataforma (moderación). */
    private void exigirCapitanOAdmin(Long equipoId, Usuario solicitante,
                                     boolean esAdmin, String accion) {
        if (esAdmin) {
            return;
        }
        boolean esCapitanActivo = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, solicitante.getId())
                .filter(m -> "CAPITAN".equals(m.getRol()) && ESTADO_ACTIVO.equals(m.getEstado()))
                .isPresent();
        if (!esCapitanActivo) {
            throw new BusinessException(
                    "Solo el capitán activo del equipo (o un administrador) puede " + accion);
        }
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }

    private Equipo buscarEquipo(Long equipoId) {
        return equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));
    }

    private void auditar(Usuario actor, String accion, Long equipoId) {
        logAuditoriaRepository.save(LogAuditoria.builder()
                .usuario(actor)
                .accion(accion)
                .entidad("equipo")
                .entidadId(equipoId)
                .fecha(LocalDateTime.now())
                .build());
    }

    private String normalizar(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }
}
