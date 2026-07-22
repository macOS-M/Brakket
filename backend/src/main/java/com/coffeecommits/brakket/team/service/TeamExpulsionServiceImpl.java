package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.ExpulsarIntegranteRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;
import com.coffeecommits.brakket.team.event.IntegranteExpulsadoEvent;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TeamExpulsionServiceImpl implements TeamExpulsionService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_EXPULSADO = "EXPULSADO";
    private static final String ROL_CAPITAN = "CAPITAN";
    private static final String EQUIPO_DISUELTO = "DISUELTO";

    private final MiembroEquipoRepository miembroEquipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TeamExpulsionServiceImpl(MiembroEquipoRepository miembroEquipoRepository,
                                    UsuarioRepository usuarioRepository,
                                    EquipoRepository equipoRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MiembroEquipoResponse expulsar(Long equipoId, Long usuarioId,
                                          ExpulsarIntegranteRequest request, String solicitanteCorreo) {

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));
        if (EQUIPO_DISUELTO.equals(equipo.getEstado())) {
            throw new BusinessException("No se pueden expulsar integrantes de un equipo disuelto");
        }

        Usuario solicitante = usuarioRepository.findByCorreo(solicitanteCorreo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", solicitanteCorreo));

        // Solo el capitan ACTIVO del equipo puede expulsar (validado en servidor).
        miembroEquipoRepository.findByEquipoIdAndUsuarioId(equipoId, solicitante.getId())
                .filter(m -> ROL_CAPITAN.equals(m.getRol()) && ESTADO_ACTIVO.equals(m.getEstado()))
                .orElseThrow(() -> new ForbiddenException(
                        "Solo el capitan activo del equipo puede expulsar integrantes"));

        MiembroEquipo objetivo = miembroEquipoRepository.findByEquipoIdAndUsuarioId(equipoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Integrante", usuarioId));

        if (!ESTADO_ACTIVO.equals(objetivo.getEstado())) {
            throw new BusinessException("Solo se puede expulsar a un integrante activo de la plantilla");
        }

        // No se puede expulsar al unico capitan sin transferir antes el rol.
        if (ROL_CAPITAN.equals(objetivo.getRol())
                && miembroEquipoRepository.countByEquipoIdAndRolAndEstado(
                        equipoId, ROL_CAPITAN, ESTADO_ACTIVO) <= 1) {
            throw new BusinessException(
                    "No se puede expulsar al unico capitan del equipo; primero transfiere el rol");
        }

        objetivo.setEstado(ESTADO_EXPULSADO);
        objetivo.setFechaBaja(LocalDateTime.now());
        objetivo.setCausaBaja(request.causa().trim());
        objetivo.setResponsableBaja(solicitante);
        miembroEquipoRepository.save(objetivo);

        // RF-10: la notificación se emite al confirmarse la baja (AFTER_COMMIT),
        // no dentro de esta transacción. Así un fallo al notificar no puede
        // marcar la transacción para rollback y deshacer la expulsión.
        eventPublisher.publishEvent(new IntegranteExpulsadoEvent(
                objetivo.getId(), objetivo.getUsuario(), equipo.getNombre(), request.causa().trim()));

        return MiembroEquipoResponse.fromEntity(objetivo);
    }
}
