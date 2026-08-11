package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.dispute.model.Apelacion;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.model.EvidenciaDisputa;
import com.coffeecommits.brakket.dispute.repository.ApelacionRepository;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.dispute.repository.EvidenciaDisputaRepository;
import com.coffeecommits.brakket.tournament.dto.EventoTrazabilidadResponse;
import com.coffeecommits.brakket.tournament.model.CasoEspecialPartida;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.repository.CasoEspecialPartidaRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RF-33: junta en una sola línea de tiempo todo lo que ya se venía
 * guardando por separado en RF-28/30/31/32 (casos especiales, disputas,
 * evidencia, apelaciones) — no inventa datos nuevos, solo los ordena.
 */
@Service
public class TrazabilidadServiceImpl implements TrazabilidadService {

    private final PartidaRepository partidaRepository;
    private final CasoEspecialPartidaRepository casoEspecialPartidaRepository;
    private final DisputaRepository disputaRepository;
    private final EvidenciaDisputaRepository evidenciaDisputaRepository;
    private final ApelacionRepository apelacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisputaGuard guard;

    public TrazabilidadServiceImpl(PartidaRepository partidaRepository,
                                   CasoEspecialPartidaRepository casoEspecialPartidaRepository,
                                   DisputaRepository disputaRepository,
                                   EvidenciaDisputaRepository evidenciaDisputaRepository,
                                   ApelacionRepository apelacionRepository,
                                   UsuarioRepository usuarioRepository,
                                   DisputaGuard guard) {
        this.partidaRepository = partidaRepository;
        this.casoEspecialPartidaRepository = casoEspecialPartidaRepository;
        this.disputaRepository = disputaRepository;
        this.evidenciaDisputaRepository = evidenciaDisputaRepository;
        this.apelacionRepository = apelacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.guard = guard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoTrazabilidadResponse> obtener(Long partidaId, String correo, boolean esAdmin) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida", partidaId));

        // Mismo criterio de siempre: no es información pública de la partida.
        guard.exigirRelacionado(partida, usuario, esAdmin);

        List<EventoTrazabilidadResponse> eventos = new ArrayList<>();

        // El resultado en sí, si ya se jugó.
        if (partida.getFechaFinalizacion() != null && partida.getGanador() != null) {
            String marcador = partida.getMarcadorA() != null && partida.getMarcadorB() != null
                    ? "%d - %d".formatted(partida.getMarcadorA(), partida.getMarcadorB())
                    : "sin marcador";
            eventos.add(new EventoTrazabilidadResponse(
                    "RESULTADO",
                    "Partida finalizada (%s). Ganó %s".formatted(marcador, partida.getGanador().getNombre()),
                    null,
                    partida.getFechaFinalizacion()));
        }

        // Descansos, avances automáticos y abandonos (RF-28).
        for (CasoEspecialPartida caso : casoEspecialPartidaRepository.findByPartidaIdOrderByFechaDesc(partidaId)) {
            eventos.add(new EventoTrazabilidadResponse(
                    "CASO_ESPECIAL_" + caso.getTipo().name(),
                    caso.getJustificacion() != null ? caso.getJustificacion() : caso.getTipo().name(),
                    caso.getRegistradoPor().getNombre(),
                    caso.getFecha()));
        }

        // Disputas, su evidencia y sus apelaciones (RF-30/31/32).
        for (Disputa disputa : disputaRepository.findByPartidaId(partidaId)) {
            eventos.add(new EventoTrazabilidadResponse(
                    "IMPUGNACION",
                    "%s: %s".formatted(disputa.getMotivo(), disputa.getDescripcion()),
                    disputa.getLevantadaPor().getNombre(),
                    disputa.getFechaCreacion()));

            for (EvidenciaDisputa evidencia : evidenciaDisputaRepository
                    .findByDisputaIdOrderByFechaCreacionAsc(disputa.getId())) {
                eventos.add(new EventoTrazabilidadResponse(
                        "EVIDENCIA",
                        evidencia.getDescripcion() != null ? evidencia.getDescripcion() : evidencia.getUrl(),
                        evidencia.getSubidoPor().getNombre(),
                        evidencia.getFechaCreacion()));
            }

            if (disputa.getResueltaPor() != null && disputa.getFechaResolucion() != null) {
                eventos.add(new EventoTrazabilidadResponse(
                        "RESOLUCION_DISPUTA",
                        "%s: %s".formatted(disputa.getDecision(), disputa.getJustificacionResolucion()),
                        disputa.getResueltaPor().getNombre(),
                        disputa.getFechaResolucion()));
            }

            for (Apelacion apelacion : apelacionRepository.findByDisputaId(disputa.getId())) {
                eventos.add(new EventoTrazabilidadResponse(
                        "APELACION",
                        apelacion.getMotivo(),
                        apelacion.getApeladaPor() != null ? apelacion.getApeladaPor().getNombre() : null,
                        apelacion.getFechaCreacion()));

                if (apelacion.getComisionado() != null && apelacion.getFechaResolucion() != null) {
                    eventos.add(new EventoTrazabilidadResponse(
                            "RESOLUCION_APELACION",
                            apelacion.getDecisionFinal() != null && !apelacion.getDecisionFinal().isBlank()
                                    ? apelacion.getDecisionFinal() : "Apelación resuelta",
                            apelacion.getComisionado().getNombre(),
                            apelacion.getFechaResolucion()));
                }
            }
        }

        return eventos.stream()
                .sorted(Comparator.comparing(EventoTrazabilidadResponse::fecha))
                .toList();
    }
}