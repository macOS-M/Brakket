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
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisputaServiceImpl implements DisputaService {

    // Regla de negocio del equipo: 48 horas desde que la partida finalizó.
    private static final long PLAZO_HORAS = 48;

    private final DisputaRepository disputaRepository;
    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisputaGuard guard;
    // RF-32: exigirArbitroOComisionado usa un criterio distinto al de guard
    // (arbitro/comisionado, sin capitanes ni organizador), asi que necesita
    // el repositorio de arbitros por separado.
    private final ArbitroTorneoRepository arbitroTorneoRepository;
    private final com.coffeecommits.brakket.tournament.service.PartidaService partidaService;

    public DisputaServiceImpl(DisputaRepository disputaRepository,
                              PartidaRepository partidaRepository,
                              UsuarioRepository usuarioRepository,
                              DisputaGuard guard,
                              ArbitroTorneoRepository arbitroTorneoRepository,
                              com.coffeecommits.brakket.tournament.service.PartidaService partidaService) {
        this.disputaRepository = disputaRepository;
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.guard = guard;
        this.arbitroTorneoRepository = arbitroTorneoRepository;
        this.partidaService = partidaService;
    }

    @Override
    @Transactional
    public DisputaResponse impugnar(Long partidaId, String correo, boolean esAdmin,
                                    ImpugnarResultadoRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        // Con lock: dos capitanes impugnando la misma partida a la vez no
        // deben poder crear dos disputas (mismo criterio que el resto del
        // motor de partidas, via bloquearPorId).
        Partida partida = partidaRepository.bloquearPorId(partidaId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida", partidaId));

        // Autorizar ANTES de revelar el estado: si no, cualquier usuario
        // autenticado podria sondear el estado de disputa de partidas de
        // torneos privados sin tener nada que ver con ellas.
        guard.exigirRelacionado(partida, usuario, esAdmin);

        if (partida.esBye()) {
            throw new BusinessException("Una partida bye (sin dos rivales reales) no se puede impugnar");
        }
        if (partida.getEstado() == EstadoPartida.EN_DISPUTA) {
            throw new BusinessException("Esta partida ya tiene una impugnacion en curso");
        }
        if (partida.getEstado() != EstadoPartida.FINALIZADA) {
            throw new BusinessException("Solo se puede impugnar un resultado ya finalizado");
        }
        // El torneo tiene que seguir EN_CURSO: resolver una disputa pasa por
        // el motor de partidas, que exige exactamente eso. Sin este corte se
        // puede impugnar la final de un torneo ya FINALIZADO y la disputa
        // queda sin nadie que pueda cerrarla, con el campeon ya coronado.
        if (partida.getTorneo().getEstado() != EstadoTorneo.EN_CURSO) {
            throw new BusinessException(
                    "Solo se puede impugnar mientras el torneo sigue en curso");
        }

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
        // de que un arbitro la resuelva.
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

        guard.exigirRelacionado(partida, usuario, esAdmin);

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

        if (!guard.estaActiva(disputa.getEstado())) {
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

    
    private void exigirArbitroOComisionado(Torneo torneo, Usuario usuario, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        boolean hayArbitros = !arbitroTorneoRepository.findByTorneoId(torneo.getId()).isEmpty();
        boolean esArbitro = arbitroTorneoRepository.findByTorneoId(torneo.getId()).stream()
                .anyMatch(a -> a.getUsuario().getId().equals(usuario.getId()));
        boolean hayComisionado = torneo.getTemporada() != null;
        boolean esComisionado = hayComisionado
                && torneo.getTemporada().getLiga().getComisionado().getId().equals(usuario.getId());
        // Ultimo recurso: el RF prefiere que el organizador no falle sus propios
        // casos, pero un torneo suelto (sin liga y sin arbitros) no tiene a nadie
        // mas. Sin esta salida la disputa quedaba trabada para siempre.
        boolean esOrganizadorDeUltimoRecurso = !hayArbitros && !hayComisionado
                && torneo.getOrganizador().getId().equals(usuario.getId());
        if (!esArbitro && !esComisionado && !esOrganizadorDeUltimoRecurso) {
            throw new ForbiddenException(
                    "Solo un arbitro del torneo, el comisionado de su liga o un admin pueden resolver la disputa");
        }
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}