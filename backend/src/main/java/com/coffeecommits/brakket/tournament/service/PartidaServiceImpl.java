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
import com.coffeecommits.brakket.tournament.model.FaseTorneo;
import com.coffeecommits.brakket.tournament.model.FormatoTorneo;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        boolean gestor = esAdmin || esOrganizador(torneo, correoOpcional);
        if (!Boolean.TRUE.equals(torneo.getPublico()) && !gestor) {
            throw new ResourceNotFoundException("Torneo", torneoId);
        }
        if (gestor) {
            return obtenerBracketInterno(torneoId);
        }
        // La clave de lobby solo viaja a los capitanes de ese cruce: es la
        // credencial para entrar a la partida privada, no un dato de espectador.
        Set<Long> misEquipos = correoOpcional == null
                ? Set.of()
                : usuarioRepository.findByCorreo(correoOpcional)
                        .map(u -> Set.copyOf(
                                inscripcionRepository.equiposCapitaneadosEnTorneo(u.getId(), torneoId)))
                        .orElse(Set.of());
        return partidaRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneoId).stream()
                .map(p -> PartidaResponse.from(p, participaEn(p, misEquipos)))
                .toList();
    }

    private static boolean participaEn(Partida p, Set<Long> equipos) {
        return (p.getEquipoA() != null && equipos.contains(p.getEquipoA().getId()))
                || (p.getEquipoB() != null && equipos.contains(p.getEquipoB().getId()));
    }

    @Override
    @Transactional
    public PartidaResponse reportar(Long partidaId, String correo, ReportarResultadoRequest request) {
        Partida partida = buscarPartidaJugable(partidaId);
        if (partida.getEstado() == EstadoPartida.EN_DISPUTA) {
            throw new BusinessException("La partida está en disputa: la resuelve el organizador");
        }
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

    // ---------- generación por formato ----------

    /** Despacha al motor del formato del torneo (cierra la deuda DD-05). */
    private void generarBracket(Torneo torneo, List<Equipo> equipos) {
        switch (FormatoTorneo.de(torneo)) {
            case ELIMINACION_DIRECTA -> generarEliminacionDirecta(torneo, equipos);
            case DOBLE_ELIMINACION -> generarDobleEliminacion(torneo, equipos);
            case ROUND_ROBIN -> crearRoundRobin(torneo, equipos, null, null);
            case SUIZO -> generarSuizo(torneo, equipos);
            case FASE_GRUPOS_Y_ELIMINACION -> generarFaseGrupos(torneo, equipos);
        }
    }

    /**
     * Eliminación directa clásica: se redondea el cupo a la potencia de 2
     * siguiente y los primeros inscritos reciben los byes.
     */
    private void generarEliminacionDirecta(Torneo torneo, List<Equipo> equipos) {
        int size = potenciaDeDos(equipos.size());
        List<List<Partida>> arbol = crearArbol(torneo, size, null, null, null);
        sembrarPrimeraRonda(arbol.get(1), equipos);
    }

    /**
     * Doble eliminación: llave superior idéntica a la directa, llave
     * inferior donde caen los perdedores (se queda fuera quien pierde dos
     * veces) y una gran final única entre ambos sobrevivientes. Sin
     * bracket reset: la gran final se juega una sola vez.
     */
    private void generarDobleEliminacion(Torneo torneo, List<Equipo> equipos) {
        int size = potenciaDeDos(equipos.size());
        int rondas = Integer.numberOfTrailingZeros(size);

        Partida granFinal = partidaRepository.save(Partida.builder()
                .torneo(torneo).fase(FaseTorneo.GRAN_FINAL).ronda(1).orden(0)
                .estado(EstadoPartida.PENDIENTE)
                .build());
        List<List<Partida>> superior =
                crearArbol(torneo, size, FaseTorneo.WINNERS, granFinal, "A");

        if (rondas == 1) {
            // Dos equipos: el perdedor de la final superior tiene su
            // revancha directamente en la gran final.
            Partida finalSuperior = superior.get(1).get(0);
            finalSuperior.setPerdedorSiguiente(granFinal);
            finalSuperior.setPerdedorSlot("B");
            partidaRepository.save(finalSuperior);
            sembrarPrimeraRonda(superior.get(1), equipos);
            return;
        }

        // Llave inferior: 2(R-1) rondas creadas de la última hacia atrás.
        // Las impares son internas (sobrevivientes entre sí) y las pares
        // reciben a los caídos de la ronda superior correspondiente.
        int rondasInferior = 2 * (rondas - 1);
        List<List<Partida>> inferior = new ArrayList<>();
        for (int i = 0; i <= rondasInferior; i++) {
            inferior.add(new ArrayList<>());
        }
        for (int q = rondasInferior; q >= 1; q--) {
            int cantidad = size >> (((q + 1) / 2) + 1);
            for (int orden = 0; orden < cantidad; orden++) {
                Partida siguiente;
                String slot;
                if (q == rondasInferior) {
                    siguiente = granFinal;
                    slot = "B";
                } else if (q % 2 == 1) {
                    // Interna → ronda de caída siguiente, mismo orden.
                    siguiente = inferior.get(q + 1).get(orden);
                    slot = "A";
                } else {
                    // De caída → interna siguiente, se comprime a la mitad.
                    siguiente = inferior.get(q + 1).get(orden / 2);
                    slot = orden % 2 == 0 ? "A" : "B";
                }
                inferior.get(q).add(partidaRepository.save(Partida.builder()
                        .torneo(torneo).fase(FaseTorneo.LOSERS).ronda(q).orden(orden)
                        .estado(EstadoPartida.PENDIENTE)
                        .siguiente(siguiente).siguienteSlot(slot)
                        .build()));
            }
        }

        // Descensos: la ronda superior 1 cae en parejas a la inferior 1;
        // la superior r (r ≥ 2) cae al slot B de la inferior 2(r-1).
        List<Partida> superior1 = superior.get(1);
        for (int o = 0; o < superior1.size(); o++) {
            Partida p = superior1.get(o);
            p.setPerdedorSiguiente(inferior.get(1).get(o / 2));
            p.setPerdedorSlot(o % 2 == 0 ? "A" : "B");
            partidaRepository.save(p);
        }
        for (int r = 2; r <= rondas; r++) {
            List<Partida> rondaSuperior = superior.get(r);
            for (int o = 0; o < rondaSuperior.size(); o++) {
                Partida p = rondaSuperior.get(o);
                p.setPerdedorSiguiente(inferior.get(2 * (r - 1)).get(o));
                p.setPerdedorSlot("B");
                partidaRepository.save(p);
            }
        }

        sembrarPrimeraRonda(superior.get(1), equipos);
    }

    /**
     * Round robin por método del círculo: un pivote fijo y el resto rota.
     * Con cantidad impar entra un hueco: quien lo toque descansa esa
     * jornada (no se crea partida). Reutilizado por la fase de grupos.
     */
    private void crearRoundRobin(Torneo torneo, List<Equipo> equipos,
                                 FaseTorneo fase, Integer grupo) {
        List<Equipo> rueda = new ArrayList<>(equipos);
        if (rueda.size() % 2 == 1) {
            rueda.add(null);
        }
        int m = rueda.size();
        for (int jornada = 1; jornada < m; jornada++) {
            int orden = 0;
            for (int i = 0; i < m / 2; i++) {
                Equipo a = rueda.get(i);
                Equipo b = rueda.get(m - 1 - i);
                if (a == null || b == null) {
                    continue;
                }
                Partida partida = Partida.builder()
                        .torneo(torneo).fase(fase).grupo(grupo)
                        .ronda(jornada).orden(orden++)
                        .equipoA(a).equipoB(b)
                        .estado(EstadoPartida.PENDIENTE)
                        .build();
                asignarLobby(partida);
                partidaRepository.save(partida);
            }
            rueda.add(1, rueda.remove(m - 1));
        }
    }

    /**
     * Suizo, ronda 1: mitad alta contra mitad baja del orden de
     * inscripción. Con cantidad impar, el último recibe un bye (vale una
     * victoria). Las rondas siguientes se generan al cerrarse la anterior.
     */
    private void generarSuizo(Torneo torneo, List<Equipo> equipos) {
        List<Equipo> lista = new ArrayList<>(equipos);
        Equipo descansa = lista.size() % 2 == 1 ? lista.remove(lista.size() - 1) : null;
        int mitad = lista.size() / 2;
        for (int i = 0; i < mitad; i++) {
            crearPartidaDeLiga(torneo, 1, i, lista.get(i), lista.get(i + mitad));
        }
        if (descansa != null) {
            cerrarByeDeLiga(torneo, 1, mitad, descansa);
        }
    }

    /**
     * Fase de grupos + eliminación: grupos de ~4 (la cantidad de grupos es
     * potencia de 2 para que la llave cierre exacta), round robin en cada
     * uno, y los dos mejores avanzan a una llave eliminatoria cruzada
     * (1° del grupo contra 2° del grupo hermano).
     */
    private void generarFaseGrupos(Torneo torneo, List<Equipo> equipos) {
        if (equipos.size() < 4) {
            throw new BusinessException(
                    "La fase de grupos necesita al menos 4 equipos inscritos (hay %d)"
                            .formatted(equipos.size()));
        }
        int grupos = Integer.highestOneBit(Math.max(1, equipos.size() / 4));
        List<List<Equipo>> porGrupo = new ArrayList<>();
        for (int g = 0; g < grupos; g++) {
            porGrupo.add(new ArrayList<>());
        }
        for (int i = 0; i < equipos.size(); i++) {
            porGrupo.get(i % grupos).add(equipos.get(i));
        }
        for (int g = 0; g < grupos; g++) {
            crearRoundRobin(torneo, porGrupo.get(g), FaseTorneo.GRUPOS, g);
        }
        // La llave queda creada pero vacía: se siembra al cerrar los grupos.
        crearArbol(torneo, grupos * 2, FaseTorneo.ELIMINACION, null, null);
    }

    // ---------- piezas comunes de generación ----------

    private static int potenciaDeDos(int n) {
        int size = Integer.highestOneBit(n);
        return size < n ? size << 1 : size;
    }

    /**
     * Árbol de eliminación creado de la final hacia atrás para poder
     * enlazar el avance. La final puede desembocar en una partida externa
     * (la gran final de la doble eliminación) vía {@code cima}/{@code slotCima}.
     */
    private List<List<Partida>> crearArbol(Torneo torneo, int size, FaseTorneo fase,
                                           Partida cima, String slotCima) {
        int rondas = Integer.numberOfTrailingZeros(size);
        List<List<Partida>> porRonda = new ArrayList<>();
        for (int i = 0; i <= rondas; i++) {
            porRonda.add(new ArrayList<>());
        }
        for (int r = rondas; r >= 1; r--) {
            int cantidad = size >> r;
            for (int orden = 0; orden < cantidad; orden++) {
                Partida siguiente = r == rondas ? cima : porRonda.get(r + 1).get(orden / 2);
                String slot = r == rondas ? slotCima : (orden % 2 == 0 ? "A" : "B");
                porRonda.get(r).add(partidaRepository.save(Partida.builder()
                        .torneo(torneo).fase(fase).ronda(r).orden(orden)
                        .estado(EstadoPartida.PENDIENTE)
                        .siguiente(siguiente)
                        .siguienteSlot(siguiente == null ? null : slot)
                        .build()));
            }
        }
        return porRonda;
    }

    /** Siembra de la ronda 1: byes primero, después los cruces reales. */
    private void sembrarPrimeraRonda(List<Partida> ronda1, List<Equipo> equipos) {
        int byes = ronda1.size() * 2 - equipos.size();
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

    /** Partida de liga (round robin / suizo): rivales conocidos y lobby de una vez. */
    private Partida crearPartidaDeLiga(Torneo torneo, int ronda, int orden,
                                       Equipo a, Equipo b) {
        Partida partida = Partida.builder()
                .torneo(torneo).ronda(ronda).orden(orden)
                .equipoA(a).equipoB(b)
                .estado(EstadoPartida.PENDIENTE)
                .build();
        asignarLobby(partida);
        return partidaRepository.save(partida);
    }

    /** Bye de ronda suiza: partida de un solo equipo que se cierra sola. */
    private void cerrarByeDeLiga(Torneo torneo, int ronda, int orden, Equipo equipo) {
        Partida bye = partidaRepository.save(Partida.builder()
                .torneo(torneo).ronda(ronda).orden(orden)
                .equipoA(equipo)
                .estado(EstadoPartida.PENDIENTE)
                .build());
        finalizar(bye, null, null);
    }

    /** Fija ganador y estado, propaga los avances y cierra la etapa si tocaba. */
    private void finalizar(Partida partida, Integer marcadorA, Integer marcadorB) {
        Equipo ganador;
        Equipo perdedor;
        if (partida.getEquipoA() == null || partida.getEquipoB() == null) {
            // Bye (o hueco doble en la llave inferior): avanza quien esté.
            ganador = partida.getEquipoA() != null ? partida.getEquipoA() : partida.getEquipoB();
            perdedor = null;
        } else {
            boolean gananA = marcadorA > marcadorB;
            ganador = gananA ? partida.getEquipoA() : partida.getEquipoB();
            perdedor = gananA ? partida.getEquipoB() : partida.getEquipoA();
        }
        partida.setGanador(ganador);
        partida.setEstado(EstadoPartida.FINALIZADA);
        partidaRepository.save(partida);

        // Re-lectura con lock: si las dos partidas que alimentan a la misma
        // siguiente terminan a la vez, la segunda espera y ve el slot que la
        // primera ya escribió. Primero se colocan ambos avances y solo
        // después se revisa cada destino: revisar antes de tiempo cerraría
        // como bye una partida cuyo otro equipo venía de esta misma jugada
        // (la gran final recibe a ganador y perdedor del mismo cruce).
        Partida destinoGanador = bloquear(partida.getSiguiente());
        Partida destinoPerdedor = bloquear(partida.getPerdedorSiguiente());
        colocar(destinoGanador, slotGanador(partida), ganador);
        colocar(destinoPerdedor, partida.getPerdedorSlot(), perdedor);
        revisarDestino(destinoGanador);
        if (destinoPerdedor != null && destinoPerdedor != destinoGanador) {
            revisarDestino(destinoPerdedor);
        }

        if (partida.getSiguiente() == null) {
            cerrarEtapa(partida, ganador);
        }
    }

    private Partida bloquear(Partida referencia) {
        return referencia == null
                ? null
                : partidaRepository.bloquearPorId(referencia.getId()).orElse(referencia);
    }

    /** Slot del ganador en su siguiente partida; filas viejas caen a orden % 2. */
    private static String slotGanador(Partida partida) {
        if (partida.getSiguienteSlot() != null) {
            return partida.getSiguienteSlot();
        }
        return partida.getOrden() % 2 == 0 ? "A" : "B";
    }

    private void colocar(Partida destino, String slot, Equipo equipo) {
        if (destino == null || equipo == null) {
            return;
        }
        if ("B".equals(slot)) {
            destino.setEquipoB(equipo);
        } else {
            destino.setEquipoA(equipo);
        }
        partidaRepository.save(destino);
    }

    /**
     * Un destino con ambos rivales recibe su lobby. Uno al que ya nadie
     * más puede alimentar (todas sus partidas de origen cerradas) y sigue
     * incompleto es un bye heredado: se cierra solo y sigue la cascada.
     */
    private void revisarDestino(Partida destino) {
        if (destino == null || destino.getEstado() != EstadoPartida.PENDIENTE) {
            return;
        }
        if (destino.getEquipoA() != null && destino.getEquipoB() != null) {
            asignarLobby(destino);
            partidaRepository.save(destino);
            return;
        }
        List<Partida> alimentadores = partidaRepository.alimentadoresDe(destino.getId());
        boolean yaNadieLlega = !alimentadores.isEmpty() && alimentadores.stream()
                .allMatch(a -> a.getEstado() == EstadoPartida.FINALIZADA
                        || a.getEstado() == EstadoPartida.CANCELADA);
        if (yaNadieLlega) {
            finalizar(destino, null, null);
        }
    }

    /**
     * Una partida sin enlace de avance terminó: qué significa depende del
     * formato. En los árboles es la final (corona); en las ligas dispara
     * la revisión de la jornada; en la fase de grupos siembra la llave.
     */
    private void cerrarEtapa(Partida partida, Equipo ganador) {
        Torneo torneo = partida.getTorneo();
        switch (FormatoTorneo.de(torneo)) {
            case ROUND_ROBIN -> intentarCerrarLiga(torneo);
            case SUIZO -> intentarAvanzarSuizo(torneo);
            case FASE_GRUPOS_Y_ELIMINACION -> {
                if (partida.getFase() == FaseTorneo.GRUPOS) {
                    intentarSembrarEliminacion(torneo);
                } else {
                    coronar(torneo, ganador);
                }
            }
            default -> coronar(torneo, ganador);
        }
    }

    private void coronar(Torneo torneo, Equipo campeon) {
        torneo.setEstado(EstadoTorneo.FINALIZADO);
        torneo.setCampeon(campeon);
        torneo.setFechaFin(LocalDateTime.now());
        torneoRepository.save(torneo);
    }

    // ---------- cierres por formato ----------

    private static boolean hayAbiertas(List<Partida> partidas) {
        return partidas.stream().anyMatch(p ->
                p.getEstado() != EstadoPartida.FINALIZADA
                        && p.getEstado() != EstadoPartida.CANCELADA);
    }

    private List<Equipo> equiposVigentes(Long torneoId) {
        return inscripcionRepository.vigentesPorTorneo(torneoId).stream()
                .map(Inscripcion::getEquipo)
                .toList();
    }

    /** Round robin: con la última jornada cerrada, el líder es campeón. */
    private void intentarCerrarLiga(Torneo torneo) {
        List<Partida> todas = partidaRepository
                .findByTorneoIdOrderByRondaAscOrdenAsc(torneo.getId());
        if (hayAbiertas(todas)) {
            return;
        }
        List<TablaPosiciones.Posicion> tabla =
                TablaPosiciones.calcular(equiposVigentes(torneo.getId()), todas);
        coronar(torneo, tabla.get(0).equipo());
    }

    /** Suizo: cerrada una ronda, se genera la siguiente o se corona al líder. */
    private void intentarAvanzarSuizo(Torneo torneo) {
        List<Partida> todas = partidaRepository
                .findByTorneoIdOrderByRondaAscOrdenAsc(torneo.getId());
        if (hayAbiertas(todas)) {
            return;
        }
        List<Equipo> equipos = equiposVigentes(torneo.getId());
        int totalRondas = rondasSuizas(equipos.size());
        int jugadas = todas.stream().mapToInt(Partida::getRonda).max().orElse(0);
        List<TablaPosiciones.Posicion> tabla = TablaPosiciones.calcular(equipos, todas);
        if (jugadas >= totalRondas) {
            coronar(torneo, tabla.get(0).equipo());
            return;
        }
        generarRondaSuiza(torneo, tabla, todas, jugadas + 1);
    }

    /** ceil(log2(n)): las rondas que el suizo necesita para un líder claro. */
    private static int rondasSuizas(int equipos) {
        return Math.max(1, 32 - Integer.numberOfLeadingZeros(equipos - 1));
    }

    /**
     * Emparejamiento suizo: por orden de tabla, cada equipo contra el
     * mejor rival disponible con el que no haya jugado (si no queda otra,
     * se admite la revancha). Con cantidad impar descansa el peor
     * clasificado que aún no haya tenido bye.
     */
    private void generarRondaSuiza(Torneo torneo, List<TablaPosiciones.Posicion> tabla,
                                   List<Partida> previas, int ronda) {
        List<Equipo> orden = new ArrayList<>(tabla.stream()
                .map(TablaPosiciones.Posicion::equipo)
                .toList());

        Set<String> enfrentados = new HashSet<>();
        Set<Long> conBye = new HashSet<>();
        for (Partida p : previas) {
            if (p.getEquipoA() != null && p.getEquipoB() != null) {
                enfrentados.add(claveCruce(p.getEquipoA(), p.getEquipoB()));
            } else if (p.getEquipoA() != null || p.getEquipoB() != null) {
                Equipo solo = p.getEquipoA() != null ? p.getEquipoA() : p.getEquipoB();
                conBye.add(solo.getId());
            }
        }

        Equipo descansa = null;
        if (orden.size() % 2 == 1) {
            for (int i = orden.size() - 1; i >= 0; i--) {
                if (!conBye.contains(orden.get(i).getId())) {
                    descansa = orden.remove(i);
                    break;
                }
            }
            if (descansa == null) {
                descansa = orden.remove(orden.size() - 1);
            }
        }

        int numero = 0;
        while (!orden.isEmpty()) {
            Equipo a = orden.remove(0);
            int rivalIndice = 0;
            for (int i = 0; i < orden.size(); i++) {
                if (!enfrentados.contains(claveCruce(a, orden.get(i)))) {
                    rivalIndice = i;
                    break;
                }
            }
            Equipo b = orden.remove(rivalIndice);
            crearPartidaDeLiga(torneo, ronda, numero++, a, b);
        }
        if (descansa != null) {
            cerrarByeDeLiga(torneo, ronda, numero, descansa);
        }
    }

    private static String claveCruce(Equipo a, Equipo b) {
        long menor = Math.min(a.getId(), b.getId());
        long mayor = Math.max(a.getId(), b.getId());
        return menor + "-" + mayor;
    }

    /**
     * Con todos los grupos cerrados se siembra la llave: el 1° de cada
     * grupo contra el 2° del grupo hermano (con un solo grupo, 1° vs 2°).
     */
    private void intentarSembrarEliminacion(Torneo torneo) {
        List<Partida> todas = partidaRepository
                .findByTorneoIdOrderByRondaAscOrdenAsc(torneo.getId());
        if (hayAbiertas(todas.stream().filter(p -> p.getFase() == FaseTorneo.GRUPOS).toList())) {
            return;
        }
        List<Partida> primeraRonda = todas.stream()
                .filter(p -> p.getFase() == FaseTorneo.ELIMINACION && p.getRonda() == 1)
                .sorted((a, b) -> a.getOrden() - b.getOrden())
                .toList();
        if (primeraRonda.isEmpty() || primeraRonda.get(0).getEquipoA() != null) {
            return; // sin llave que sembrar o ya sembrada
        }

        int grupos = primeraRonda.size();
        List<Equipo> vigentes = equiposVigentes(torneo.getId());
        List<List<TablaPosiciones.Posicion>> tablas = new ArrayList<>();
        for (int g = 0; g < grupos; g++) {
            final int indice = g;
            List<Partida> delGrupo = todas.stream()
                    .filter(p -> p.getFase() == FaseTorneo.GRUPOS
                            && Objects.equals(p.getGrupo(), indice))
                    .toList();
            Set<Long> miembros = new HashSet<>();
            for (Partida p : delGrupo) {
                if (p.getEquipoA() != null) {
                    miembros.add(p.getEquipoA().getId());
                }
                if (p.getEquipoB() != null) {
                    miembros.add(p.getEquipoB().getId());
                }
            }
            tablas.add(TablaPosiciones.calcular(
                    vigentes.stream().filter(e -> miembros.contains(e.getId())).toList(),
                    delGrupo));
        }

        for (int j = 0; j < grupos; j++) {
            Partida cruce = primeraRonda.get(j);
            List<TablaPosiciones.Posicion> propia = tablas.get(j);
            List<TablaPosiciones.Posicion> hermana = tablas.get(grupos == 1 ? 0 : j ^ 1);
            cruce.setEquipoA(propia.get(0).equipo());
            cruce.setEquipoB(hermana.get(1).equipo());
            asignarLobby(cruce);
            partidaRepository.save(cruce);
        }
    }

    /**
     * El puente con el juego real: Brakket no se conecta al juego, genera
     * las credenciales de la partida privada que ambos capitanes usan.
     */
    private void asignarLobby(Partida partida) {
        if (partida.getLobbyNombre() != null) {
            return;
        }
        partida.setLobbyNombre("BRAKKET-T%d-%sR%dM%d".formatted(
                partida.getTorneo().getId(), marcaDeFase(partida),
                partida.getRonda(), partida.getOrden() + 1));
        StringBuilder clave = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            clave.append(ALFABETO_CLAVE[RANDOM.nextInt(ALFABETO_CLAVE.length)]);
        }
        partida.setLobbyClave(clave.toString());
    }

    /** Distingue la lobby entre fases que repiten ronda y orden (W1 vs L1, G1 vs G2). */
    private static String marcaDeFase(Partida partida) {
        if (partida.getFase() == null) {
            return "";
        }
        return switch (partida.getFase()) {
            case WINNERS -> "W";
            case LOSERS -> "L";
            case GRAN_FINAL -> "GF";
            case GRUPOS -> "G" + (partida.getGrupo() + 1) + "-";
            case ELIMINACION -> "E";
        };
    }

    // ---------- helpers ----------

    private List<PartidaResponse> obtenerBracketInterno(Long torneoId) {
        return partidaRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneoId).stream()
                .map(PartidaResponse::from)
                .toList();
    }

    /**
     * Partida sobre la que se puede actuar: torneo EN_CURSO y rivales
     * definidos. Se lee con bloqueo de fila para que dos acciones
     * simultáneas (confirmar vs rechazar, doble reporte) se serialicen.
     */
    private Partida buscarPartidaJugable(Long partidaId) {
        Partida partida = partidaRepository.bloquearPorId(partidaId)
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
                    "No hay empates: los marcadores deben diferir para definir un ganador");
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
