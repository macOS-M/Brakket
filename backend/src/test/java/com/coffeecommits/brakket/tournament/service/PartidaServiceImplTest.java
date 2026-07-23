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
                torneoRepository, partidaRepository, inscripcionRepository, usuarioRepository);

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
