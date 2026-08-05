package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.dto.PartidaResponse;
import com.coffeecommits.brakket.tournament.dto.ReportarResultadoRequest;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Inscripcion;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.CasoEspecialPartidaRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidaServiceImplTest {

    @Mock
    private TorneoRepository torneoRepository;
    @Mock
    private PartidaRepository partidaRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ArbitroTorneoRepository arbitroTorneoRepository;
    @Mock
    private CasoEspecialPartidaRepository casoEspecialPartidaRepository;

    private PartidaServiceImpl service;

    /** Las partidas "persistidas" del test, con ids incrementales. */
    private final List<Partida> guardadas = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);

    private static final String ORGANIZADOR = "orga@brakket.gg";
    private static final String CAPITAN_A = "capa@brakket.gg";
    private static final String CAPITAN_B = "capb@brakket.gg";

    private Torneo torneo;

    @BeforeEach
    void setUp() {
        service = new PartidaServiceImpl(
                torneoRepository, partidaRepository, inscripcionRepository, usuarioRepository,
                arbitroTorneoRepository, casoEspecialPartidaRepository);

        torneo = Torneo.builder()
                .id(7L)
                .juego(Juego.builder().id(3L).nombre("Rocket League").build())
                .organizador(Usuario.builder().id(1L).nombre("Orga").correo(ORGANIZADOR).build())
                .nombre("Copa").formato("Eliminación directa")
                .tamanoEquipo(2).maxEquipos(8)
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .estado(EstadoTorneo.INSCRIPCION_ABIERTA)
                .publico(true)
                .build();

        lenient().when(torneoRepository.findById(7L)).thenReturn(Optional.of(torneo));
        lenient().when(torneoRepository.save(any(Torneo.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(usuarioRepository.findByCorreo(ORGANIZADOR))
                .thenReturn(Optional.of(torneo.getOrganizador()));
        lenient().when(partidaRepository.save(any(Partida.class))).thenAnswer(inv -> {
            Partida p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(ids.incrementAndGet());
                guardadas.add(p);
            }
            return p;
        });
        lenient().when(partidaRepository.findByTorneoIdOrderByRondaAscOrdenAsc(7L))
                .thenAnswer(inv -> guardadas.stream()
                        .sorted((a, b) -> a.getRonda().equals(b.getRonda())
                                ? a.getOrden() - b.getOrden()
                                : a.getRonda() - b.getRonda())
                        .toList());
        // La lectura con lock resuelve contra las "persistidas" del test.
        lenient().when(partidaRepository.bloquearPorId(any(Long.class))).thenAnswer(inv ->
                guardadas.stream()
                        .filter(g -> g.getId().equals(inv.<Long>getArgument(0)))
                        .findFirst());
        lenient().when(partidaRepository.alimentadoresDe(any(Long.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return guardadas.stream()
                    .filter(g -> (g.getSiguiente() != null && id.equals(g.getSiguiente().getId()))
                            || (g.getPerdedorSiguiente() != null
                                    && id.equals(g.getPerdedorSiguiente().getId())))
                    .toList();
        });
    }

    private List<Equipo> equipos(int n) {
        List<Equipo> lista = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            lista.add(Equipo.builder().id((long) i).nombre("Equipo " + i).build());
        }
        return lista;
    }

    private void inscribir(List<Equipo> equipos) {
        when(inscripcionRepository.vigentesPorTorneo(7L)).thenReturn(equipos.stream()
                .map(e -> Inscripcion.builder().torneo(torneo).equipo(e)
                        .estado("CONFIRMADA").fechaSolicitud(LocalDate.now()).build())
                .toList());
    }

    @Test
    void iniciar_con_4_equipos_genera_dos_rondas_con_lobby_y_pone_en_curso() {
        inscribir(equipos(4));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.EN_CURSO);
        assertThat(bracket).hasSize(3); // 2 semifinales + 1 final
        List<PartidaResponse> ronda1 = bracket.stream().filter(p -> p.ronda() == 1).toList();
        assertThat(ronda1).hasSize(2);
        // Sin byes: ambas semifinales tienen rivales y credenciales de lobby.
        assertThat(ronda1).allSatisfy(p -> {
            assertThat(p.equipoAId()).isNotNull();
            assertThat(p.equipoBId()).isNotNull();
            assertThat(p.lobbyNombre()).startsWith("BRAKKET-T7-R1");
            assertThat(p.lobbyClave()).hasSize(8);
            assertThat(p.estado()).isEqualTo("PENDIENTE");
        });
        // La final espera a los ganadores: sin equipos ni lobby todavía.
        PartidaResponse fin = bracket.stream().filter(p -> p.ronda() == 2).findFirst().orElseThrow();
        assertThat(fin.equipoAId()).isNull();
        assertThat(fin.lobbyNombre()).isNull();
    }

    @Test
    void iniciar_con_5_equipos_reparte_byes_que_avanzan_solos() {
        inscribir(equipos(5));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        // Cupo redondeado a 8: 4 + 2 + 1 partidas.
        assertThat(bracket).hasSize(7);
        List<PartidaResponse> byes = bracket.stream().filter(PartidaResponse::bye).toList();
        assertThat(byes).hasSize(3);
        // Los tres byes quedan finalizados y sus equipos ya ocupan semifinales.
        assertThat(byes).allSatisfy(p -> assertThat(p.estado()).isEqualTo("FINALIZADA"));
        List<PartidaResponse> semis = bracket.stream().filter(p -> p.ronda() == 2).toList();
        long slotsOcupados = semis.stream()
                .mapToLong(p -> (p.equipoAId() != null ? 1 : 0) + (p.equipoBId() != null ? 1 : 0))
                .sum();
        assertThat(slotsOcupados).isEqualTo(3);
        // La semifinal de los dos primeros byes ya tiene lobby (rivales completos).
        assertThat(semis.stream().filter(p -> p.equipoAId() != null && p.equipoBId() != null))
                .allSatisfy(p -> assertThat(p.lobbyNombre()).isNotNull());
    }

    @Test
    void iniciar_exige_organizador_y_al_menos_dos_equipos() {
        when(usuarioRepository.findByCorreo("otro@x.com"))
                .thenReturn(Optional.of(Usuario.builder().id(99L).build()));
        assertThatThrownBy(() -> service.iniciarTorneo(7L, "otro@x.com", false))
                .isInstanceOf(ForbiddenException.class);

        inscribir(equipos(1));
        assertThatThrownBy(() -> service.iniciarTorneo(7L, ORGANIZADOR, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos 2");
    }

    // ---------- resultados ----------

    /** Partida 1v2 de un torneo EN_CURSO con capitanes 10 (A) y 20 (B). */
    private Partida partidaJugable() {
        return partidaJugable(0);
    }

    private Partida partidaJugable(int orden) {
        torneo.setEstado(EstadoTorneo.EN_CURSO);
        Partida fin = Partida.builder().id(300L).torneo(torneo).ronda(2).orden(0)
                .estado(EstadoPartida.PENDIENTE).build();
        Partida p = Partida.builder().id(200L).torneo(torneo).ronda(1).orden(orden)
                .equipoA(Equipo.builder().id(10L).nombre("Azules").build())
                .equipoB(Equipo.builder().id(20L).nombre("Rojos").build())
                .estado(EstadoPartida.PENDIENTE)
                .siguiente(fin)
                .build();
        lenient().when(partidaRepository.bloquearPorId(200L)).thenReturn(Optional.of(p));
        lenient().when(partidaRepository.bloquearPorId(300L)).thenReturn(Optional.of(fin));
        lenient().when(usuarioRepository.findByCorreo(CAPITAN_A))
                .thenReturn(Optional.of(Usuario.builder().id(10L).correo(CAPITAN_A).build()));
        lenient().when(usuarioRepository.findByCorreo(CAPITAN_B))
                .thenReturn(Optional.of(Usuario.builder().id(20L).correo(CAPITAN_B).build()));
        lenient().when(inscripcionRepository.esCapitanActivo(10L, 10L)).thenReturn(true);
        lenient().when(inscripcionRepository.esCapitanActivo(10L, 20L)).thenReturn(false);
        lenient().when(inscripcionRepository.esCapitanActivo(20L, 10L)).thenReturn(false);
        lenient().when(inscripcionRepository.esCapitanActivo(20L, 20L)).thenReturn(true);
        return p;
    }

    @Test
    void reportar_y_confirmar_avanza_al_ganador_en_la_llave() {
        Partida p = partidaJugable();

        PartidaResponse reportada = service.reportar(200L, CAPITAN_A,
                new ReportarResultadoRequest(3, 1));
        assertThat(reportada.estado()).isEqualTo("REPORTADA");
        assertThat(reportada.reportadoPorEquipoId()).isEqualTo(10L);

        // El propio reportador no puede confirmarse a sí mismo.
        assertThatThrownBy(() -> service.confirmar(200L, CAPITAN_A))
                .isInstanceOf(ForbiddenException.class);

        PartidaResponse confirmada = service.confirmar(200L, CAPITAN_B);
        assertThat(confirmada.estado()).isEqualTo("FINALIZADA");
        assertThat(confirmada.ganadorEquipoId()).isEqualTo(10L);
        // El ganador ocupó el slot A de la final (orden 0 → slot A).
        assertThat(p.getSiguiente().getEquipoA().getId()).isEqualTo(10L);
    }

    @Test
    void confirmar_la_final_corona_al_campeon_y_finaliza_el_torneo() {
        Partida p = partidaJugable();
        p.setSiguiente(null); // esta partida ES la final

        service.reportar(200L, CAPITAN_B, new ReportarResultadoRequest(0, 2));
        service.confirmar(200L, CAPITAN_A);

        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.FINALIZADO);
        assertThat(torneo.getCampeon().getId()).isEqualTo(20L);
        assertThat(torneo.getFechaFin()).isNotNull();
    }

    @Test
    void rechazo_deja_en_disputa_y_el_organizador_resuelve() {
        Partida p = partidaJugable();

        service.reportar(200L, CAPITAN_A, new ReportarResultadoRequest(3, 1));
        PartidaResponse disputada = service.rechazar(200L, CAPITAN_B);
        assertThat(disputada.estado()).isEqualTo("EN_DISPUTA");

        // Un tercero no puede resolver; el organizador sí.
        when(usuarioRepository.findByCorreo("otro@x.com"))
                .thenReturn(Optional.of(Usuario.builder().id(99L).build()));
        assertThatThrownBy(() -> service.resolver(200L, "otro@x.com", false,
                new ReportarResultadoRequest(1, 2)))
                .isInstanceOf(ForbiddenException.class);

        PartidaResponse resuelta = service.resolver(200L, ORGANIZADOR, false,
                new ReportarResultadoRequest(1, 2));
        assertThat(resuelta.estado()).isEqualTo("FINALIZADA");
        assertThat(resuelta.ganadorEquipoId()).isEqualTo(20L);
    }

    @Test
    void reportar_rechaza_empates_y_partidas_sin_rivales_definidos() {
        Partida p = partidaJugable();
        assertThatThrownBy(() -> service.reportar(200L, CAPITAN_A,
                new ReportarResultadoRequest(2, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empates");

        p.setEquipoB(null);
        assertThatThrownBy(() -> service.reportar(200L, CAPITAN_A,
                new ReportarResultadoRequest(2, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rondas previas");
    }

    @Test
    void iniciar_con_2_equipos_genera_solo_la_final() {
        inscribir(equipos(2));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        assertThat(bracket).hasSize(1);
        PartidaResponse fin = bracket.get(0);
        assertThat(fin.ronda()).isEqualTo(1);
        assertThat(fin.equipoAId()).isNotNull();
        assertThat(fin.equipoBId()).isNotNull();
        assertThat(fin.lobbyNombre()).isNotNull();
        assertThat(fin.estado()).isEqualTo("PENDIENTE");
    }

    @Test
    void iniciar_con_3_equipos_manda_el_bye_directo_a_la_final() {
        inscribir(equipos(3));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        // Cupo 4: un bye (finalizado) + una semifinal real + la final.
        assertThat(bracket).hasSize(3);
        PartidaResponse bye = bracket.stream().filter(PartidaResponse::bye).findFirst().orElseThrow();
        assertThat(bye.estado()).isEqualTo("FINALIZADA");
        PartidaResponse fin = bracket.stream().filter(p -> p.ronda() == 2).findFirst().orElseThrow();
        // El primer inscrito (bye, orden 0) ya espera en el slot A de la final.
        assertThat(fin.equipoAId()).isEqualTo(bye.equipoAId());
        assertThat(fin.equipoBId()).isNull();
        assertThat(fin.lobbyNombre()).isNull();
    }

    @Test
    void ganador_de_partida_con_orden_impar_avanza_al_slot_b() {
        Partida p = partidaJugable(1);

        service.reportar(200L, CAPITAN_A, new ReportarResultadoRequest(3, 1));
        service.confirmar(200L, CAPITAN_B);

        assertThat(p.getSiguiente().getEquipoB().getId()).isEqualTo(10L);
        assertThat(p.getSiguiente().getEquipoA()).isNull();
    }

    @Test
    void doble_reporte_y_reporte_sobre_disputa_quedan_bloqueados() {
        Partida p = partidaJugable();

        service.reportar(200L, CAPITAN_A, new ReportarResultadoRequest(3, 1));
        assertThatThrownBy(() -> service.reportar(200L, CAPITAN_B,
                new ReportarResultadoRequest(1, 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene un resultado");

        service.rechazar(200L, CAPITAN_B);
        assertThatThrownBy(() -> service.reportar(200L, CAPITAN_A,
                new ReportarResultadoRequest(3, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disputa");
    }

    @Test
    void resolver_tambien_rechaza_empates() {
        partidaJugable();
        assertThatThrownBy(() -> service.resolver(200L, ORGANIZADOR, false,
                new ReportarResultadoRequest(1, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empates");
    }

    @Test
    void una_partida_cerrada_o_de_torneo_finalizado_no_admite_acciones() {
        Partida p = partidaJugable();
        p.setEstado(EstadoPartida.FINALIZADA);
        assertThatThrownBy(() -> service.reportar(200L, CAPITAN_A,
                new ReportarResultadoRequest(2, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cerrada");

        p.setEstado(EstadoPartida.PENDIENTE);
        torneo.setEstado(EstadoTorneo.FINALIZADO);
        assertThatThrownBy(() -> service.confirmar(200L, CAPITAN_B))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está en curso");
    }

    // ---------- formatos nuevos (DD-05) ----------

    /** Partida "persistida" por fase/ronda/orden, para asserts de estructura. */
    private Partida buscar(String fase, int ronda, int orden) {
        return guardadas.stream()
                .filter(p -> (p.getFase() == null ? fase == null
                        : p.getFase().name().equals(fase))
                        && p.getRonda() == ronda && p.getOrden() == orden)
                .findFirst().orElseThrow();
    }

    /**
     * Juega el torneo completo como organizador: en cada cruce gana el
     * equipo de menor id (2-0). Con inscritos en orden, el campeón de
     * cualquier formato debe ser el Equipo 1.
     */
    private void jugarTodoGanaMenorId() {
        while (true) {
            Partida pendiente = guardadas.stream()
                    .filter(p -> p.getEstado() == EstadoPartida.PENDIENTE
                            || p.getEstado() == EstadoPartida.REPORTADA)
                    .filter(p -> p.getEquipoA() != null && p.getEquipoB() != null)
                    .findFirst().orElse(null);
            if (pendiente == null) {
                return;
            }
            boolean gananA = pendiente.getEquipoA().getId() < pendiente.getEquipoB().getId();
            service.resolver(pendiente.getId(), ORGANIZADOR, false,
                    new ReportarResultadoRequest(gananA ? 2 : 0, gananA ? 0 : 2));
        }
    }

    @Test
    void doble_eliminacion_con_4_equipos_arma_ambas_llaves_y_gran_final() {
        torneo.setFormato("DOBLE_ELIMINACION");
        inscribir(equipos(4));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        // 2 semis + final superior, 2 rondas de inferior y la gran final.
        assertThat(bracket).hasSize(6);
        assertThat(bracket.stream().filter(p -> "WINNERS".equals(p.fase()))).hasSize(3);
        assertThat(bracket.stream().filter(p -> "LOSERS".equals(p.fase()))).hasSize(2);
        assertThat(bracket.stream().filter(p -> "GRAN_FINAL".equals(p.fase()))).hasSize(1);

        // Cae la semifinal 1: el perdedor baja al slot A de la inferior 1.
        service.resolver(buscar("WINNERS", 1, 0).getId(), ORGANIZADOR, false,
                new ReportarResultadoRequest(2, 0));
        assertThat(buscar("LOSERS", 1, 0).getEquipoA().getId()).isEqualTo(2L);

        jugarTodoGanaMenorId();

        // Con "gana el menor id": la inferior la sobrevive el 2, que pierde
        // la gran final contra el invicto 1. Perder dos veces elimina.
        Partida granFinal = buscar("GRAN_FINAL", 1, 0);
        assertThat(granFinal.getEquipoA().getId()).isEqualTo(1L);
        assertThat(granFinal.getEquipoB().getId()).isEqualTo(2L);
        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.FINALIZADO);
        assertThat(torneo.getCampeon().getId()).isEqualTo(1L);
    }

    @Test
    void doble_eliminacion_con_3_equipos_hereda_el_bye_en_la_llave_inferior() {
        torneo.setFormato("DOBLE_ELIMINACION");
        inscribir(equipos(3));

        service.iniciarTorneo(7L, ORGANIZADOR, false);

        // El bye de la superior no manda a nadie a la inferior todavía.
        Partida inferior1 = buscar("LOSERS", 1, 0);
        assertThat(inferior1.getEstado()).isEqualTo(EstadoPartida.PENDIENTE);

        // Cerrada la otra semifinal, la inferior 1 queda sin segundo rival
        // posible: se hereda el bye y su único equipo avanza solo.
        service.resolver(buscar("WINNERS", 1, 1).getId(), ORGANIZADOR, false,
                new ReportarResultadoRequest(2, 0)); // gana 2, cae 3
        assertThat(inferior1.getEstado()).isEqualTo(EstadoPartida.FINALIZADA);
        assertThat(inferior1.getGanador().getId()).isEqualTo(3L);
        assertThat(buscar("LOSERS", 2, 0).getEquipoA().getId()).isEqualTo(3L);

        jugarTodoGanaMenorId();
        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.FINALIZADO);
        assertThat(torneo.getCampeon().getId()).isEqualTo(1L);
    }

    @Test
    void round_robin_todos_contra_todos_y_el_lider_es_campeon() {
        torneo.setFormato("ROUND_ROBIN");
        inscribir(equipos(4));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        // 3 jornadas de 2 cruces, todos con rivales y lobby desde el inicio.
        assertThat(bracket).hasSize(6);
        assertThat(bracket).allSatisfy(p -> {
            assertThat(p.equipoAId()).isNotNull();
            assertThat(p.equipoBId()).isNotNull();
            assertThat(p.lobbyNombre()).isNotNull();
        });
        // Cada equipo juega exactamente 3 veces.
        long aparicionesDe1 = bracket.stream()
                .filter(p -> p.equipoAId() == 1L || p.equipoBId() == 1L).count();
        assertThat(aparicionesDe1).isEqualTo(3);

        jugarTodoGanaMenorId();
        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.FINALIZADO);
        assertThat(torneo.getCampeon().getId()).isEqualTo(1L);
    }

    @Test
    void suizo_genera_rondas_al_cierre_y_evita_revanchas() {
        torneo.setFormato("SUIZO");
        inscribir(equipos(4));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        // Ronda 1: mitad alta contra mitad baja (1v3 y 2v4); nada más aún.
        assertThat(bracket).hasSize(2);
        assertThat(buscar(null, 1, 0).getEquipoB().getId()).isEqualTo(3L);
        assertThat(buscar(null, 1, 1).getEquipoB().getId()).isEqualTo(4L);

        // Al cerrar la ronda se genera la 2: líderes entre sí, sin revancha.
        jugarTodoGanaMenorId();
        assertThat(buscar(null, 2, 0).getEquipoA().getId()).isEqualTo(1L);
        assertThat(buscar(null, 2, 0).getEquipoB().getId()).isEqualTo(2L);

        // ceil(log2(4)) = 2 rondas y se corona al invicto.
        assertThat(guardadas.stream().mapToInt(Partida::getRonda).max().orElse(0)).isEqualTo(2);
        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.FINALIZADO);
        assertThat(torneo.getCampeon().getId()).isEqualTo(1L);
    }

    @Test
    void suizo_impar_reparte_byes_sin_repetirlos() {
        torneo.setFormato("SUIZO");
        inscribir(equipos(3));

        service.iniciarTorneo(7L, ORGANIZADOR, false);

        // Ronda 1: 1v2 y bye (victoria automática) para el 3.
        Partida bye1 = buscar(null, 1, 1);
        assertThat(bye1.esBye()).isTrue();
        assertThat(bye1.getGanador().getId()).isEqualTo(3L);

        jugarTodoGanaMenorId();

        // Ronda 2: el bye no se repite (le toca al 2) y el 1 sigue jugando.
        Partida bye2 = guardadas.stream()
                .filter(p -> p.getRonda() == 2 && p.esBye())
                .findFirst().orElseThrow();
        assertThat(bye2.getGanador().getId()).isEqualTo(2L);
        assertThat(torneo.getCampeon().getId()).isEqualTo(1L);
    }

    @Test
    void fase_de_grupos_siembra_la_llave_cruzada_al_cerrar_los_grupos() {
        torneo.setFormato("FASE_GRUPOS_Y_ELIMINACION");
        inscribir(equipos(8));

        List<PartidaResponse> bracket = service.iniciarTorneo(7L, ORGANIZADOR, false);

        // 2 grupos de 4 (reparto alterno): 6 partidas por grupo, y una
        // llave de 4 (2 semis + final) esperando vacía.
        assertThat(bracket.stream().filter(p -> "GRUPOS".equals(p.fase()))).hasSize(12);
        List<PartidaResponse> llave = bracket.stream()
                .filter(p -> "ELIMINACION".equals(p.fase())).toList();
        assertThat(llave).hasSize(3);
        assertThat(llave).allSatisfy(p -> assertThat(p.equipoAId()).isNull());

        // Se juegan solo los grupos: gana siempre el menor id.
        while (guardadas.stream().anyMatch(p -> p.getFase() != null
                && p.getFase().name().equals("GRUPOS")
                && p.getEstado() == EstadoPartida.PENDIENTE)) {
            Partida pendiente = guardadas.stream()
                    .filter(p -> p.getFase() != null && p.getFase().name().equals("GRUPOS")
                            && p.getEstado() == EstadoPartida.PENDIENTE)
                    .findFirst().orElseThrow();
            boolean gananA = pendiente.getEquipoA().getId() < pendiente.getEquipoB().getId();
            service.resolver(pendiente.getId(), ORGANIZADOR, false,
                    new ReportarResultadoRequest(gananA ? 2 : 0, gananA ? 0 : 2));
        }

        // Grupos por reparto alterno: G1={1,3,5,7}, G2={2,4,6,8}. Cruce:
        // 1° de cada grupo contra el 2° del hermano.
        Partida semi1 = buscar("ELIMINACION", 1, 0);
        Partida semi2 = buscar("ELIMINACION", 1, 1);
        assertThat(semi1.getEquipoA().getId()).isEqualTo(1L);
        assertThat(semi1.getEquipoB().getId()).isEqualTo(4L);
        assertThat(semi2.getEquipoA().getId()).isEqualTo(2L);
        assertThat(semi2.getEquipoB().getId()).isEqualTo(3L);
        assertThat(semi1.getLobbyNombre()).isNotNull();

        jugarTodoGanaMenorId();
        assertThat(torneo.getEstado()).isEqualTo(EstadoTorneo.FINALIZADO);
        assertThat(torneo.getCampeon().getId()).isEqualTo(1L);
    }

    @Test
    void fase_de_grupos_exige_al_menos_4_equipos() {
        torneo.setFormato("FASE_GRUPOS_Y_ELIMINACION");
        inscribir(equipos(3));

        assertThatThrownBy(() -> service.iniciarTorneo(7L, ORGANIZADOR, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos 4");
    }

    @Test
    void el_bracket_publico_oculta_la_clave_de_lobby_a_quien_no_es_capitan_del_cruce() {
        Partida p = partidaJugable();
        p.setLobbyNombre("BRAKKET-T7-R1M1");
        p.setLobbyClave("abcd2345");
        when(partidaRepository.findByTorneoIdOrderByRondaAscOrdenAsc(7L)).thenReturn(List.of(p));

        // Espectador anónimo: ve el cruce y el nombre, nunca la clave.
        List<PartidaResponse> anonimo = service.obtenerBracket(7L, null, false);
        assertThat(anonimo.get(0).lobbyNombre()).isEqualTo("BRAKKET-T7-R1M1");
        assertThat(anonimo.get(0).lobbyClave()).isNull();

        // Capitán de un equipo del cruce: la clave viaja.
        when(inscripcionRepository.equiposCapitaneadosEnTorneo(10L, 7L)).thenReturn(List.of(10L));
        List<PartidaResponse> capitan = service.obtenerBracket(7L, CAPITAN_A, false);
        assertThat(capitan.get(0).lobbyClave()).isEqualTo("abcd2345");

        // El organizador ve todas las claves.
        List<PartidaResponse> organizador = service.obtenerBracket(7L, ORGANIZADOR, false);
        assertThat(organizador.get(0).lobbyClave()).isEqualTo("abcd2345");
    }
}
