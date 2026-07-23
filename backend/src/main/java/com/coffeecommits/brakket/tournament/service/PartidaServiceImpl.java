package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.dto.PartidaResponse;
import com.coffeecommits.brakket.tournament.dto.ReportarResultadoRequest;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Inscripcion;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PartidaServiceImpl implements PartidaService {

    /** Sin caracteres confusos (0/O, 1/l/I) para dictarla por voz en la lobby. */
    private static final char[] ALFABETO_CLAVE = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TorneoRepository torneoRepository;
    private final PartidaRepository partidaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;

    public PartidaServiceImpl(TorneoRepository torneoRepository,
                              PartidaRepository partidaRepository,
                              InscripcionRepository inscripcionRepository,
                              UsuarioRepository usuarioRepository) {
        this.torneoRepository = torneoRepository;
        this.partidaRepository = partidaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public List<PartidaResponse> iniciarTorneo(Long torneoId, String correo, boolean esAdmin) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo", torneoId));
        exigirOrganizador(torneo, correo, esAdmin,
                "Solo el organizador o un administrador pueden iniciar el torneo");

        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_ABIERTA) {
            throw new BusinessException("El torneo no está en etapa de inscripción");
        }
        if (partidaRepository.existsByTorneoId(torneoId)) {
            throw new BusinessException("El bracket de este torneo ya fue generado");
        }

        List<Equipo> equipos = inscripcionRepository.vigentesPorTorneo(torneoId).stream()
                .map(Inscripcion::getEquipo)
                .toList();
        if (equipos.size() < 2) {
            throw new BusinessException(
                    "Se necesitan al menos 2 equipos inscritos para iniciar (hay %d)"
                            .formatted(equipos.size()));
        }

        generarBracket(torneo, equipos);
        torneo.setEstado(EstadoTorneo.EN_CURSO);
        torneoRepository.save(torneo);

        return obtenerBracketInterno(torneoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartidaResponse> obtenerBracket(Long torneoId, String correoOpcional, boolean esAdmin) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo", torneoId));
        // Misma visibilidad que el torneo: uno privado solo existe para su dueño.
        if (!Boolean.TRUE.equals(torneo.getPublico()) && !esAdmin
                && !esOrganizador(torneo, correoOpcional)) {
            throw new ResourceNotFoundException("Torneo", torneoId);
        }
        return obtenerBracketInterno(torneoId);
    }

    @Override
    @Transactional
    public PartidaResponse reportar(Long partidaId, String correo, ReportarResultadoRequest request) {
        Partida partida = buscarPartidaJugable(partidaId);
        if (partida.getEstado() != EstadoPartida.PENDIENTE) {
            throw new BusinessException("La partida ya tiene un resultado reportado");
        }
        validarMarcadores(request);

        Equipo miEquipo = equipoDelCapitan(partida, correo);
        partida.setMarcadorA(request.marcadorA());
        partida.setMarcadorB(request.marcadorB());
        partida.setReportadoPor(miEquipo);
        partida.setEstado(EstadoPartida.REPORTADA);
        return PartidaResponse.from(partidaRepository.save(partida));
    }

    @Override
    @Transactional
    public PartidaResponse confirmar(Long partidaId, String correo) {
        Partida partida = buscarPartidaJugable(partidaId);
        exigirCapitanRival(partida, correo);
        finalizar(partida, partida.getMarcadorA(), partida.getMarcadorB());
        return PartidaResponse.from(partida);
    }

    @Override
    @Transactional
    public PartidaResponse rechazar(Long partidaId, String correo) {
        Partida partida = buscarPartidaJugable(partidaId);
        exigirCapitanRival(partida, correo);
        partida.setEstado(EstadoPartida.EN_DISPUTA);
        return PartidaResponse.from(partidaRepository.save(partida));
    }

    @Override
    @Transactional
    public PartidaResponse resolver(Long partidaId, String correo, boolean esAdmin,
                                    ReportarResultadoRequest request) {
        Partida partida = buscarPartidaJugable(partidaId);
        exigirOrganizador(partida.getTorneo(), correo, esAdmin,
                "Solo el organizador o un administrador pueden resolver la partida");
        validarMarcadores(request);
        partida.setMarcadorA(request.marcadorA());
        partida.setMarcadorB(request.marcadorB());
        finalizar(partida, request.marcadorA(), request.marcadorB());
        return PartidaResponse.from(partida);
    }

    // ---------- generación del bracket ----------

    /**
     * Eliminación directa clásica: se redondea el cupo a la potencia de 2
     * siguiente y los primeros inscritos reciben los byes. Las rondas se
     * crean de la final hacia atrás para poder enlazar el avance.
     */
    private void generarBracket(Torneo torneo, List<Equipo> equipos) {
        int n = equipos.size();
        int size = Integer.highestOneBit(n);
        if (size < n) {
            size <<= 1;
        }
        int rondas = Integer.numberOfTrailingZeros(size);
        int byes = size - n;

        List<List<Partida>> porRonda = new ArrayList<>();
        for (int i = 0; i <= rondas; i++) {
            porRonda.add(new ArrayList<>());
        }
        for (int r = rondas; r >= 1; r--) {
            int cantidad = size >> r;
            for (int orden = 0; orden < cantidad; orden++) {
                Partida siguiente = r == rondas ? null : porRonda.get(r + 1).get(orden / 2);
                Partida partida = Partida.builder()
                        .torneo(torneo)
                        .ronda(r)
                        .orden(orden)
                        .estado(EstadoPartida.PENDIENTE)
                        .siguiente(siguiente)
                        .build();
                porRonda.get(r).add(partidaRepository.save(partida));
            }
        }

        // Siembra de la ronda 1: byes primero, después los cruces reales.
        List<Partida> ronda1 = porRonda.get(1);
        int siguienteEquipo = 0;
        for (int orden = 0; orden < ronda1.size(); orden++) {
            Partida partida = ronda1.get(orden);
            partida.setEquipoA(equipos.get(siguienteEquipo++));
            if (orden >= byes) {
                partida.setEquipoB(equipos.get(siguienteEquipo++));
                asignarLobby(partida);
            }
            partidaRepository.save(partida);
        }
        // Los byes se finalizan solos y su equipo avanza de una vez.
        for (int orden = 0; orden < byes; orden++) {
            finalizar(ronda1.get(orden), null, null);
        }
    }

    /** Fija ganador y estado, y hace avanzar la llave (o corona al campeón). */
    private void finalizar(Partida partida, Integer marcadorA, Integer marcadorB) {
        Equipo ganador;
        if (partida.getEquipoB() == null) {
            ganador = partida.getEquipoA(); // bye
        } else {
            ganador = marcadorA > marcadorB ? partida.getEquipoA() : partida.getEquipoB();
        }
        partida.setGanador(ganador);
        partida.setEstado(EstadoPartida.FINALIZADA);
        partidaRepository.save(partida);

        Partida siguiente = partida.getSiguiente();
        if (siguiente == null) {
            Torneo torneo = partida.getTorneo();
            torneo.setEstado(EstadoTorneo.FINALIZADO);
            torneo.setCampeon(ganador);
            torneo.setFechaFin(LocalDateTime.now());
            torneoRepository.save(torneo);
            return;
        }
        if (partida.getOrden() % 2 == 0) {
            siguiente.setEquipoA(ganador);
        } else {
            siguiente.setEquipoB(ganador);
        }
        if (siguiente.getEquipoA() != null && siguiente.getEquipoB() != null) {
            asignarLobby(siguiente);
        }
        partidaRepository.save(siguiente);
    }

    /**
     * El puente con el juego real: Brakket no se conecta al juego, genera
     * las credenciales de la partida privada que ambos capitanes usan.
     */
    private void asignarLobby(Partida partida) {
        if (partida.getLobbyNombre() != null) {
            return;
        }
        partida.setLobbyNombre("BRAKKET-T%d-R%dM%d".formatted(
                partida.getTorneo().getId(), partida.getRonda(), partida.getOrden() + 1));
        StringBuilder clave = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            clave.append(ALFABETO_CLAVE[RANDOM.nextInt(ALFABETO_CLAVE.length)]);
        }
        partida.setLobbyClave(clave.toString());
    }

    // ---------- helpers ----------

    private List<PartidaResponse> obtenerBracketInterno(Long torneoId) {
        return partidaRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneoId).stream()
                .map(PartidaResponse::from)
                .toList();
    }

    /** Partida sobre la que se puede actuar: torneo EN_CURSO y rivales definidos. */
    private Partida buscarPartidaJugable(Long partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida", partidaId));
        if (partida.getTorneo().getEstado() != EstadoTorneo.EN_CURSO) {
            throw new BusinessException("El torneo no está en curso");
        }
        if (partida.getEstado() == EstadoPartida.FINALIZADA
                || partida.getEstado() == EstadoPartida.CANCELADA) {
            throw new BusinessException("La partida ya está cerrada");
        }
        if (partida.getEquipoA() == null || partida.getEquipoB() == null) {
            throw new BusinessException("La partida aún espera rivales de rondas previas");
        }
        return partida;
    }

    private void validarMarcadores(ReportarResultadoRequest request) {
        if (Objects.equals(request.marcadorA(), request.marcadorB())) {
            throw new BusinessException(
                    "En eliminación directa no hay empates: los marcadores deben diferir");
        }
    }

    /** El equipo de esta partida cuyo capitán activo es el usuario. */
    private Equipo equipoDelCapitan(Partida partida, String correo) {
        Usuario usuario = buscarUsuario(correo);
        if (inscripcionRepository.esCapitanActivo(usuario.getId(), partida.getEquipoA().getId())) {
            return partida.getEquipoA();
        }
        if (inscripcionRepository.esCapitanActivo(usuario.getId(), partida.getEquipoB().getId())) {
            return partida.getEquipoB();
        }
        throw new ForbiddenException("Solo un capitán de la partida puede reportar el resultado");
    }

    /** Confirmar/rechazar es del capitán del equipo que NO reportó. */
    private void exigirCapitanRival(Partida partida, String correo) {
        if (partida.getEstado() != EstadoPartida.REPORTADA) {
            throw new BusinessException("No hay un reporte pendiente de confirmación");
        }
        Equipo miEquipo = equipoDelCapitan(partida, correo);
        if (Objects.equals(miEquipo.getId(), partida.getReportadoPor().getId())) {
            throw new ForbiddenException("El resultado lo confirma o rechaza el capitán rival");
        }
    }

    private void exigirOrganizador(Torneo torneo, String correo, boolean esAdmin, String mensaje) {
        if (!esAdmin && !esOrganizador(torneo, correo)) {
            throw new ForbiddenException(mensaje);
        }
    }

    private boolean esOrganizador(Torneo torneo, String correoOpcional) {
        if (correoOpcional == null) {
            return false;
        }
        return usuarioRepository.findByCorreo(correoOpcional)
                .map(u -> Objects.equals(u.getId(), torneo.getOrganizador().getId()))
                .orElse(false);
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
    }
}
