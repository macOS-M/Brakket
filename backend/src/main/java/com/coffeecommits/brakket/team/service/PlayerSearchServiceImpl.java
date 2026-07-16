package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.JugadorDisponibleResponse;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerSearchServiceImpl implements PlayerSearchService {

    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;

    public PlayerSearchServiceImpl(UsuarioRepository usuarioRepository,
                                   EquipoRepository equipoRepository,
                                   MiembroEquipoRepository miembroEquipoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JugadorDisponibleResponse> buscar(Long equipoId, String texto, Long juegoId,
                                                  boolean soloDisponibles, String capitanCorreo) {

        equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        Usuario capitan = usuarioRepository.findByCorreo(capitanCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", capitanCorreo));

        MiembroEquipo miembroCapitan = miembroEquipoRepository
                .findByEquipoIdAndUsuarioId(equipoId, capitan.getId())
                .orElseThrow(() -> new BusinessException("No tiene permisos sobre este equipo"));

        if (!"CAPITAN".equals(miembroCapitan.getRol())) {
            throw new BusinessException("Solo el capitan del equipo puede buscar jugadores");
        }

        String textoFiltro = (texto == null || texto.isBlank()) ? "" : texto.trim();

        return usuarioRepository.buscarDisponibles(textoFiltro, juegoId).stream()
                .filter(candidato -> !esYaMiembroActivo(equipoId, candidato.getId()))
                .map(candidato -> mapearCandidato(equipoId, candidato))
                .filter(r -> !soloDisponibles || !r.requiereTransferencia())
                .toList();
    }

    private boolean esYaMiembroActivo(Long equipoId, Long usuarioId) {
        return miembroEquipoRepository.findByEquipoIdAndUsuarioId(equipoId, usuarioId)
                .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                .isPresent();
    }

    private JugadorDisponibleResponse mapearCandidato(Long equipoId, Usuario candidato) {
        Optional<MiembroEquipo> otroEquipoActivo = miembroEquipoRepository
                .findByUsuarioId(candidato.getId()).stream()
                .filter(m -> ESTADO_ACTIVO.equals(m.getEstado()))
                .filter(m -> !m.getEquipo().getId().equals(equipoId))
                .findFirst();

        return otroEquipoActivo
                .map(m -> JugadorDisponibleResponse.conTransferencia(candidato, m.getEquipo().getNombre()))
                .orElseGet(() -> JugadorDisponibleResponse.invitacionDirecta(candidato));
    }
}