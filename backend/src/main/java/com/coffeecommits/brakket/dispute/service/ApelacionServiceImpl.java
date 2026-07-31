package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.dispute.dto.ApelacionResponse;
import com.coffeecommits.brakket.dispute.dto.ApelarRequest;
import com.coffeecommits.brakket.dispute.dto.ResolverApelacionRequest;
import com.coffeecommits.brakket.dispute.model.Apelacion;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.repository.ApelacionRepository;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.service.PartidaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApelacionServiceImpl implements ApelacionService {

    // Mismo plazo que RF-30 usa para impugnar: 48h, ahora desde que se
    // resolvió la disputa.
    private static final long PLAZO_HORAS = 48;

    private final ApelacionRepository apelacionRepository;
    private final DisputaRepository disputaRepository;
    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ArbitroTorneoRepository arbitroTorneoRepository;
    private final PartidaService partidaService;

    public ApelacionServiceImpl(ApelacionRepository apelacionRepository,
                                DisputaRepository disputaRepository,
                                PartidaRepository partidaRepository,
                                UsuarioRepository usuarioRepository,
                                InscripcionRepository inscripcionRepository,
                                ArbitroTorneoRepository arbitroTorneoRepository,
                                PartidaService partidaService) {
        this.apelacionRepository = apelacionRepository;
        this.disputaRepository = disputaRepository;
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.arbitroTorneoRepository = arbitroTorneoRepository;
        this.partidaService = partidaService;
    }

    @Override
    @Transactional
    public ApelacionResponse apelar(Long disputaId, String correo, boolean esAdmin, ApelarRequest request) {
        Usuario usuario = buscarUsuario(correo);
        Disputa disputa = disputaRepository.findById(disputaId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa", disputaId));
        Partida partida = disputa.getPartida();

        if (!"RESUELTA".equals(disputa.getEstado())) {
            throw new BusinessException("Solo se puede apelar una disputa ya resuelta");
        }
        exigirRelacionado(partida, usuario, esAdmin);

        LocalDateTime limite = disputa.getFechaResolucion() == null
                ? null : disputa.getFechaResolucion().plusHours(PLAZO_HORAS);
        if (limite == null || LocalDateTime.now().isAfter(limite)) {
            throw new BusinessException(
                    "El plazo de %d horas para apelar esta resolucion ya vencio".formatted(PLAZO_HORAS));
        }

        // Una sola apelación por disputa, sin importar su estado: ya
        // resuelta o pendiente, no se puede volver a apelar la misma.
        boolean yaFueApelada = !apelacionRepository.findByDisputaId(disputaId).isEmpty();
        if (yaFueApelada) {
            throw new BusinessException("Esta disputa ya fue apelada anteriormente");
        }

        Apelacion apelacion = apelacionRepository.save(Apelacion.builder()
                .disputa(disputa)
                .apeladaPor(usuario)
                .motivo(request.motivo().trim())
                .estado("PENDIENTE")
                .fechaCreacion(LocalDateTime.now())
                .build());

        // Reabre el caso: el comisionado necesita poder tocar la partida
        // otra vez si decide corregir el resultado.
        disputa.setEstado("EN_APELACION");
        disputaRepository.save(disputa);
        partida.setEstado(EstadoPartida.EN_DISPUTA);
        partidaRepository.save(partida);

        return ApelacionResponse.fromEntity(apelacion);
    }

    @Override
    @Transactional
    public ApelacionResponse resolver(Long apelacionId, String correo, boolean esAdmin,
                                      ResolverApelacionRequest request) {
        Usuario usuario = buscarUsuario(correo);
        Apelacion apelacion = apelacionRepository.findById(apelacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Apelacion", apelacionId));
        Disputa disputa = apelacion.getDisputa();
        Torneo torneo = disputa.getPartida().getTorneo();

        if (!"PENDIENTE".equals(apelacion.getEstado())) {
            throw new BusinessException("Esta apelacion ya fue resuelta");
        }
        exigirComisionado(torneo, usuario, esAdmin);

        // El comisionado puede confirmar lo que decidió el árbitro
        // (equipoGanadorId null) o corregirlo una vez más.
        partidaService.finalizarPorResolucionDeDisputa(disputa.getPartida().getId(), request.equipoGanadorId());

        apelacion.setDecisionFinal(normalizar(request.decisionFinal()));
        apelacion.setComisionado(usuario);
        apelacion.setEstado("RESUELTA");
        apelacion.setFechaResolucion(LocalDateTime.now());
        apelacionRepository.save(apelacion);

        disputa.setEstado("RESUELTA");
        disputaRepository.save(disputa);

        return ApelacionResponse.fromEntity(apelacion);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<ApelacionResponse> listarPorDisputa(Long disputaId) {
        return apelacionRepository.findByDisputaId(disputaId).stream()
                .map(ApelacionResponse::fromEntity)
                .toList();
    }

    /**
     * La apelación escala por encima del árbitro: solo el comisionado de
     * la liga, o un admin si el torneo no tiene liga/comisionado.
     */
    private void exigirComisionado(Torneo torneo, Usuario usuario, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        boolean esComisionado = torneo.getTemporada() != null
                && torneo.getTemporada().getLiga().getComisionado().getId().equals(usuario.getId());
        if (!esComisionado) {
            throw new ForbiddenException(
                    "Solo el comisionado de la liga o un admin pueden resolver la apelacion");
        }
    }

    // Mismo criterio que ya usa DisputaServiceImpl para impugnar/evidencia.
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
                    "Solo un capitan de la partida, el organizador o un arbitro del torneo pueden apelar");
        }
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}