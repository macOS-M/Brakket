package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
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

    public DisputaServiceImpl(DisputaRepository disputaRepository,
                              PartidaRepository partidaRepository,
                              UsuarioRepository usuarioRepository,
                              InscripcionRepository inscripcionRepository,
                              ArbitroTorneoRepository arbitroTorneoRepository) {
        this.disputaRepository = disputaRepository;
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.arbitroTorneoRepository = arbitroTorneoRepository;
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
        exigirRelacionado(partida, usuario, esAdmin);

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
        // de que un arbitro la resuelva (eso ya lo hara RF-32).
        partida.setEstado(EstadoPartida.EN_DISPUTA);
        partidaRepository.save(partida);

        return DisputaResponse.fromEntity(disputa);
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