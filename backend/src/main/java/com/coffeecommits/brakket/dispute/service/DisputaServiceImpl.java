package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;
import com.coffeecommits.brakket.dispute.dto.ResolverDisputaRequest;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisputaServiceImpl implements DisputaService {

    // Regla de negocio del equipo: 48 horas desde que la partida finalizó.
    private static final long PLAZO_HORAS = 48;
    private static final List<String> ESTADOS_ACTIVOS = List.of("PENDIENTE", "EN_REVISION");

    private final DisputaRepository disputaRepository;
    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ArbitroTorneoRepository arbitroTorneoRepository;
    private final com.coffeecommits.brakket.tournament.service.PartidaService partidaService;

    public DisputaServiceImpl(DisputaRepository disputaRepository,
                              PartidaRepository partidaRepository,
                              UsuarioRepository usuarioRepository,
                              InscripcionRepository inscripcionRepository,
                              ArbitroTorneoRepository arbitroTorneoRepository,
                              com.coffeecommits.brakket.tournament.service.PartidaService partidaService) {
        this.disputaRepository = disputaRepository;
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.arbitroTorneoRepository = arbitroTorneoRepository;
        this.partidaService = partidaService;
    }

    @Override
    @Transactional
    public DisputaResponse impugnar(Long partidaId, String correo, boolean esAdmin,
                                    ImpugnarResultadoRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida", partidaId));

        if (partida.getEstado() == EstadoPartida.EN_DISPUTA) {
            throw new BusinessException("Esta partida ya tiene una impugnacion en curso");
        }
        if (partida.getEstado() != EstadoPartida.FINALIZADA) {
            throw new BusinessException("Solo se puede impugnar un resultado ya finalizado");
        }

        exigirRelacionado(partida, usuario, esAdmin);

        // Partidas de antes de RF-30 no tienen fecha guardada: sin forma
        // de calcular el plazo, se tratan como vencidas por seguridad.
        LocalDateTime limite = partida.getFechaFinalizacion() == null
                ? null : partida.getFechaFinalizacion().plusHours(PLAZO_HORAS);
        if (limite == null || LocalDateTime.now().isAfter(limite)) {
            throw new BusinessException(
                    "El plazo de %d horas para impugnar este resultado ya vencio".formatted(PLAZO_HORAS));
        }
        Disputa disputa = disputaRepository.save(Disputa.builder()
                .partida(partida)
                .levantadaPor(usuario)
                .motivo(request.motivo().trim())
                .descripcion(request.descripcion().trim())
                .evidenciaUrl(normalizar(request.evidenciaUrl()))
                .estado("PENDIENTE")
                .fechaCreacion(LocalDateTime.now())
                .build());

        // Queda igual que un reporte rechazado: EN_DISPUTA, a la espera
        // de que un arbitro la resuelva (eso ya lo hara RF-32).
        partida.setEstado(EstadoPartida.EN_DISPUTA);
        partidaRepository.save(partida);
        return DisputaResponse.fromEntity(disputa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputaResponse> listarPorPartida(Long partidaId, String correo, boolean esAdmin) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida", partidaId));

        exigirRelacionado(partida, usuario, esAdmin);

        return disputaRepository.findByPartidaId(partidaId).stream()
                .map(DisputaResponse::fromEntity)
                .toList();
    }
    @Override
    @Transactional
    public DisputaResponse resolver(Long disputaId, String correo, boolean esAdmin,
                                    ResolverDisputaRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        Disputa disputa = disputaRepository.findById(disputaId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa", disputaId));

        if (!ESTADOS_ACTIVOS.contains(disputa.getEstado())) {
            throw new BusinessException("Esta disputa ya fue resuelta");
        }
        exigirArbitroOComisionado(disputa.getPartida().getTorneo(), usuario, esAdmin);

        String decision = request.decision() == null ? "" : request.decision().trim().toUpperCase();
        if (!"MANTENER".equals(decision) && !"REVERTIR".equals(decision)) {
            throw new BusinessException("La decision debe ser MANTENER o REVERTIR");
        }
        if ("REVERTIR".equals(decision) && request.equipoGanadorId() == null) {
            throw new BusinessException("Revertir requiere indicar el equipo que gana");
        }

        // Reutiliza el motor de la llave; ahí se bloquea si revertir ya
        // no es seguro (la llave avanzó más allá de este cruce).
        partidaService.finalizarPorResolucionDeDisputa(disputa.getPartida().getId(),
                "REVERTIR".equals(decision) ? request.equipoGanadorId() : null);

        disputa.setDecision(decision);
        disputa.setJustificacionResolucion(request.justificacion().trim());
        disputa.setSancion(normalizar(request.sancion()));
        disputa.setResueltaPor(usuario);
        disputa.setFechaResolucion(LocalDateTime.now());
        disputa.setEstado("RESUELTA");
        disputaRepository.save(disputa);

        return DisputaResponse.fromEntity(disputa);
    }

    /**
     * A propósito NO incluye al organizador: RF-32 exige que resuelva
     * alguien ajeno al torneo (árbitro, comisionado de la liga, o admin).
     */
    private void exigirArbitroOComisionado(Torneo torneo, Usuario usuario, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        boolean esArbitro = arbitroTorneoRepository.findByTorneoId(torneo.getId()).stream()
                .anyMatch(a -> a.getUsuario().getId().equals(usuario.getId()));
        boolean esComisionado = torneo.getTemporada() != null
                && torneo.getTemporada().getLiga().getComisionado().getId().equals(usuario.getId());
        if (!esArbitro && !esComisionado) {
            throw new ForbiddenException(
                    "Solo un arbitro del torneo, el comisionado de su liga o un admin pueden resolver la disputa");
        }
    }

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
                    "Solo un capitan de la partida, el organizador o un arbitro del torneo pueden impugnar");
        }
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}