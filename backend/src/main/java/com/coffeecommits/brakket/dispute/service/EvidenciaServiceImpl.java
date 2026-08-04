package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.dispute.dto.AdjuntarEvidenciaRequest;
import com.coffeecommits.brakket.dispute.dto.EvidenciaResponse;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.model.EvidenciaDisputa;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.dispute.repository.EvidenciaDisputaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service

public class EvidenciaServiceImpl implements EvidenciaService {

    private final DisputaRepository disputaRepository;
    private final EvidenciaDisputaRepository evidenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisputaGuard guard;

    public EvidenciaServiceImpl(DisputaRepository disputaRepository,
                                EvidenciaDisputaRepository evidenciaRepository,
                                UsuarioRepository usuarioRepository,
                                DisputaGuard guard) {
        this.disputaRepository = disputaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.guard = guard;
    }

    @Override
    @Transactional
    public EvidenciaResponse adjuntar(Long disputaId, String correo, boolean esAdmin,
                                      AdjuntarEvidenciaRequest request) {
        Usuario usuario = buscarUsuario(correo);
        Disputa disputa = buscarDisputa(disputaId);

        if (!guard.estaActiva(disputa.getEstado())) {
            throw new BusinessException("Esta disputa ya no esta abierta; no se puede adjuntar mas evidencia");
        }
        guard.exigirRelacionado(disputa.getPartida(), usuario, esAdmin);

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

        guard.exigirRelacionado(disputa.getPartida(), usuario, esAdmin);

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

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}