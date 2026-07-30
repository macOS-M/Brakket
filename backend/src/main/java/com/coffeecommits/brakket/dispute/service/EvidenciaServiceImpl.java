package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.dispute.dto.AdjuntarEvidenciaRequest;
import com.coffeecommits.brakket.dispute.dto.EvidenciaResponse;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.model.EvidenciaDisputa;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.dispute.repository.EvidenciaDisputaRepository;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EvidenciaServiceImpl implements EvidenciaService {

    private static final List<String> ESTADOS_ACTIVOS = List.of("PENDIENTE", "EN_REVISION");

    private final DisputaRepository disputaRepository;
    private final EvidenciaDisputaRepository evidenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ArbitroTorneoRepository arbitroTorneoRepository;

    public EvidenciaServiceImpl(DisputaRepository disputaRepository,
                                EvidenciaDisputaRepository evidenciaRepository,
                                UsuarioRepository usuarioRepository,
                                InscripcionRepository inscripcionRepository,
                                ArbitroTorneoRepository arbitroTorneoRepository) {
        this.disputaRepository = disputaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.arbitroTorneoRepository = arbitroTorneoRepository;
    }

    @Override
    @Transactional
    public EvidenciaResponse adjuntar(Long disputaId, String correo, boolean esAdmin,
                                      AdjuntarEvidenciaRequest request) {
        Usuario usuario = buscarUsuario(correo);
        Disputa disputa = buscarDisputa(disputaId);

        if (!ESTADOS_ACTIVOS.contains(disputa.getEstado())) {
            throw new BusinessException("Esta disputa ya no esta abierta; no se puede adjuntar mas evidencia");
        }
        exigirRelacionado(disputa.getPartida(), usuario, esAdmin);

        EvidenciaDisputa evidencia = evidenciaRepository.save(EvidenciaDisputa.builder()
                .disputa(disputa)
                .subidoPor(usuario)
                .url(request.url().trim())
                .descripcion(normalizar(request.descripcion()))
                .fechaCreacion(LocalDateTime.now())
                .build());

        return EvidenciaResponse.fromEntity(evidencia);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenciaResponse> listar(Long disputaId, String correo, boolean esAdmin) {
        Usuario usuario = buscarUsuario(correo);
        Disputa disputa = buscarDisputa(disputaId);

        exigirRelacionado(disputa.getPartida(), usuario, esAdmin);

        return evidenciaRepository.findByDisputaIdOrderByFechaCreacionAsc(disputaId).stream()
                .map(EvidenciaResponse::fromEntity)
                .toList();
    }

    private Disputa buscarDisputa(Long disputaId) {
        return disputaRepository.findById(disputaId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa", disputaId));
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }

    // Mismo criterio que ya usa DisputaServiceImpl para impugnar: capitan
    // de cualquiera de los 2 equipos, organizador, arbitro o admin.
    private void exigirRelacionado(Partida partida, Usuario usuario, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        Torneo torneo = partida.getTorneo();
        boolean esOrganizador = torneo.getOrganizador().getId().equals(usuario.getId());
        boolean esArbitro = arbitroTorneoRepository.findByTorneoId(torneo.getId()).stream()
                .anyMatch(a -> a.getUsuario().getId().equals(usuario.getId()));
        boolean esCapitanA = partida.getEquipoA() != null
                && inscripcionRepository.esCapitanActivo(usuario.getId(), partida.getEquipoA().getId());
        boolean esCapitanB = partida.getEquipoB() != null
                && inscripcionRepository.esCapitanActivo(usuario.getId(), partida.getEquipoB().getId());

        if (!esOrganizador && !esArbitro && !esCapitanA && !esCapitanB) {
            throw new ForbiddenException(
                    "Solo un capitan de la partida, el organizador o un arbitro del torneo pueden ver o adjuntar evidencia");
        }
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}