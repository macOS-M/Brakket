package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.AsignarRolRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;
import com.coffeecommits.brakket.team.model.EquipoRolHistorial;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.EquipoRolHistorialRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TeamRoleServiceImpl implements TeamRoleService {

    private static final Set<String> ROLES_VALIDOS = Set.of("CAPITAN", "TITULAR", "SUPLENTE", "COACH");
    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final MiembroEquipoRepository miembroEquipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRolHistorialRepository historialRepository;
    private final EquipoRepository equipoRepository;

    public TeamRoleServiceImpl(MiembroEquipoRepository miembroEquipoRepository,
                               UsuarioRepository usuarioRepository,
                               EquipoRolHistorialRepository historialRepository,
                               EquipoRepository equipoRepository) {
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialRepository = historialRepository;
        this.equipoRepository = equipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiembroEquipoResponse> listarMiembros(Long equipoId) {
        // RF-08: el equipo consultado debe existir; si esta disuelto se devuelve
        // igualmente su plantilla como vista historica.
        if (!equipoRepository.existsById(equipoId)) {
            throw new ResourceNotFoundException("Equipo", equipoId);
        }
        return miembroEquipoRepository.findByEquipoId(equipoId).stream()
                .map(MiembroEquipoResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public MiembroEquipoResponse cambiarRol(Long equipoId, Long miembroUsuarioId,
                                            AsignarRolRequest request, String solicitanteCorreo) {

        String nuevoRol = request.nuevoRol().toUpperCase();
        if (!ROLES_VALIDOS.contains(nuevoRol)) {
            throw new BusinessException(
                    "Rol invalido '%s'. Los roles permitidos son: %s".formatted(nuevoRol, ROLES_VALIDOS));
        }

        Usuario solicitante = usuarioRepository.findByCorreo(solicitanteCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", solicitanteCorreo));

        MiembroEquipo capitanSolicitante = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, solicitante.getId())
                .orElseThrow(() -> new BusinessException("No tiene permisos sobre este equipo"));

        if (!"CAPITAN".equals(capitanSolicitante.getRol())) {
            throw new BusinessException("Solo el capitan del equipo puede asignar roles");
        }

        MiembroEquipo miembro = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, miembroUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Integrante", miembroUsuarioId));

        if (!ESTADO_ACTIVO.equals(miembro.getEstado())) {
            throw new BusinessException("El integrante no esta activo en el equipo");
        }

        String rolAnterior = miembro.getRol();

        if (rolAnterior.equals(nuevoRol)) {
            return MiembroEquipoResponse.fromEntity(miembro);
        }

        if ("CAPITAN".equals(rolAnterior)) {
            long capitanesActivos = miembroEquipoRepository
                    .countByEquipoIdAndRolAndEstado(equipoId, "CAPITAN", ESTADO_ACTIVO);
            if (capitanesActivos <= 1) {
                throw new BusinessException(
                        "El equipo debe mantener al menos un capitan activo");
            }
        }

        miembro.setRol(nuevoRol);
        miembroEquipoRepository.save(miembro);

        EquipoRolHistorial historial = EquipoRolHistorial.builder()
                .equipo(miembro.getEquipo())
                .usuario(miembro.getUsuario())
                .rolAnterior(rolAnterior)
                .rolNuevo(nuevoRol)
                .fecha(LocalDateTime.now())
                .responsable(solicitante)
                .build();
        historialRepository.save(historial);

        return MiembroEquipoResponse.fromEntity(miembro);
    }
}