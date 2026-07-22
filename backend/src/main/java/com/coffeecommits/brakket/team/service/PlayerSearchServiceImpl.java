package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.JugadorDisponibleResponse;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlayerSearchServiceImpl implements PlayerSearchService {

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
    public PageResponse<JugadorDisponibleResponse> buscar(Long equipoId, String texto, Long juegoId,
                                                          boolean soloDisponibles, String capitanCorreo,
                                                          int page, int size) {

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

        // La query aplica todos los filtros (incluye la exclusion de miembros y,
        // si soloDisponibles, la de jugadores en otro equipo), asi que la pagina
        // ya llega filtrada y con el total correcto.
        Page<Usuario> pagina = usuarioRepository.buscarDisponibles(
                textoFiltro, juegoId, equipoId, soloDisponibles, PageRequest.of(page, size));

        // Una sola query para los badges de toda la pagina, en vez de una por
        // candidato: aca estaba el N+1.
        List<Long> ids = pagina.getContent().stream().map(Usuario::getId).toList();
        Map<Long, String> equipoActualPorUsuario = ids.isEmpty()
                ? Map.of()
                : miembroEquipoRepository.findActivosByUsuarioIds(ids).stream()
                        .filter(m -> !m.getEquipo().getId().equals(equipoId))
                        .collect(Collectors.toMap(
                                m -> m.getUsuario().getId(),
                                m -> m.getEquipo().getNombre(),
                                (primero, segundo) -> primero));

        return PageResponse.from(pagina, usuario -> {
            String equipoActual = equipoActualPorUsuario.get(usuario.getId());
            return equipoActual != null
                    ? JugadorDisponibleResponse.conTransferencia(usuario, equipoActual)
                    : JugadorDisponibleResponse.invitacionDirecta(usuario);
        });
    }
}
