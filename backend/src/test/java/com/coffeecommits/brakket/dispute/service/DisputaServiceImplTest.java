package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.dispute.dto.DisputaResponse;
import com.coffeecommits.brakket.dispute.dto.ImpugnarResultadoRequest;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.model.ArbitroTorneo;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputaServiceImplTest {

    @Mock
    private DisputaRepository disputaRepository;
    @Mock
    private PartidaRepository partidaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private ArbitroTorneoRepository arbitroTorneoRepository;

    private DisputaServiceImpl service;

    private static final String ORGANIZADOR = "orga@brakket.gg";
    private static final String CAPITAN_A = "capa@brakket.gg";
    private static final String CAPITAN_B = "capb@brakket.gg";
    private static final String ARBITRO = "arbitro@brakket.gg";
    private static final String AJENO = "ajeno@brakket.gg";

    private Torneo torneo;
    private Equipo equipoA;
    private Equipo equipoB;

    @BeforeEach
    void setUp() {
        service = new DisputaServiceImpl(
                disputaRepository, partidaRepository, usuarioRepository,
                inscripcionRepository, arbitroTorneoRepository);

        // EN_CURSO: impugnar exige que el torneo siga abierto, porque
        // resolver la disputa pasa por el motor de partidas, que lo exige.
        torneo = Torneo.builder().id(7L)
                .organizador(Usuario.builder().id(1L).correo(ORGANIZADOR).build())
                .estado(EstadoTorneo.EN_CURSO)
                .build();
        equipoA = Equipo.builder().id(10L).nombre("Azules").build();
        equipoB = Equipo.builder().id(20L).nombre("Rojos").build();

        lenient().when(usuarioRepository.findByCorreo(ORGANIZADOR))
                .thenReturn(Optional.of(torneo.getOrganizador()));
        lenient().when(usuarioRepository.findByCorreo(CAPITAN_A))
                .thenReturn(Optional.of(Usuario.builder().id(10L).correo(CAPITAN_A).build()));
        lenient().when(usuarioRepository.findByCorreo(CAPITAN_B))
                .thenReturn(Optional.of(Usuario.builder().id(20L).correo(CAPITAN_B).build()));
        lenient().when(usuarioRepository.findByCorreo(ARBITRO))
                .thenReturn(Optional.of(Usuario.builder().id(50L).correo(ARBITRO).build()));
        lenient().when(usuarioRepository.findByCorreo(AJENO))
                .thenReturn(Optional.of(Usuario.builder().id(99L).correo(AJENO).build()));

        lenient().when(inscripcionRepository.esCapitanActivo(10L, 10L)).thenReturn(true);
        lenient().when(inscripcionRepository.esCapitanActivo(20L, 20L)).thenReturn(true);
        lenient().when(disputaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(partidaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Partida finalizada hace N horas, con ambos rivales, lista para impugnar. */
    private Partida partidaFinalizadaHaceHoras(long horas) {
        Partida p = Partida.builder().id(200L).torneo(torneo)
                .equipoA(equipoA).equipoB(equipoB)
                .estado(EstadoPartida.FINALIZADA)
                .fechaFinalizacion(LocalDateTime.now().minusHours(horas))
                .build();
        lenient().when(partidaRepository.bloquearPorId(200L)).thenReturn(Optional.of(p));
        return p;
    }

    private ImpugnarResultadoRequest request() {
        return new ImpugnarResultadoRequest("Marcador incorrecto", "El resultado no es correcto", null);
    }

    // ---------- matriz de permisos ----------

    @Test
    void capitan_organizador_y_arbitro_pueden_impugnar_pero_un_ajeno_no() {
        Usuario arbitroUsuario = Usuario.builder().id(50L).correo(ARBITRO).build();
        when(arbitroTorneoRepository.findByTorneoId(7L)).thenReturn(List.of(
                ArbitroTorneo.builder().id(1L).torneo(torneo).usuario(arbitroUsuario).build()));

        partidaFinalizadaHaceHoras(1);
        assertThat(service.impugnar(200L, CAPITAN_A, false, request()).estado()).isEqualTo("PENDIENTE");

        partidaFinalizadaHaceHoras(1);
        assertThat(service.impugnar(200L, ORGANIZADOR, false, request()).estado()).isEqualTo("PENDIENTE");

        partidaFinalizadaHaceHoras(1);
        assertThat(service.impugnar(200L, ARBITRO, false, request()).estado()).isEqualTo("PENDIENTE");

        partidaFinalizadaHaceHoras(1);
        assertThatThrownBy(() -> service.impugnar(200L, AJENO, false, request()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void un_admin_puede_impugnar_aunque_no_tenga_relacion_con_la_partida() {
        partidaFinalizadaHaceHoras(1);
        assertThat(service.impugnar(200L, AJENO, true, request()).estado()).isEqualTo("PENDIENTE");
    }

    // ---------- plazo de 48 horas ----------

    @Test
    void impugnar_dentro_del_plazo_de_48_horas_funciona_y_fuera_del_plazo_se_bloquea() {
        partidaFinalizadaHaceHoras(47);
        assertThat(service.impugnar(200L, CAPITAN_A, false, request()).estado()).isEqualTo("PENDIENTE");

        partidaFinalizadaHaceHoras(49);
        assertThatThrownBy(() -> service.impugnar(200L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("plazo");
    }

    @Test
    void sin_fecha_de_finalizacion_se_trata_como_vencido() {
        Partida p = Partida.builder().id(200L).torneo(torneo)
                .equipoA(equipoA).equipoB(equipoB)
                .estado(EstadoPartida.FINALIZADA)
                .fechaFinalizacion(null)
                .build();
        when(partidaRepository.bloquearPorId(200L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.impugnar(200L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("plazo");
    }

    // ---------- estados ----------

    @Test
    void solo_se_puede_impugnar_un_resultado_ya_finalizado() {
        Partida p = Partida.builder().id(200L).torneo(torneo)
                .equipoA(equipoA).equipoB(equipoB)
                .estado(EstadoPartida.PENDIENTE)
                .build();
        when(partidaRepository.bloquearPorId(200L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.impugnar(200L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("finalizado");
    }

    @Test
    void no_se_puede_impugnar_dos_veces_la_misma_partida() {
        Partida p = partidaFinalizadaHaceHoras(1);
        p.setEstado(EstadoPartida.EN_DISPUTA);

        assertThatThrownBy(() -> service.impugnar(200L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("impugnacion en curso");
    }

    @Test
    void una_partida_bye_no_se_puede_impugnar() {
        Partida p = Partida.builder().id(200L).torneo(torneo)
                .equipoA(equipoA).equipoB(null)
                .estado(EstadoPartida.FINALIZADA)
                .fechaFinalizacion(LocalDateTime.now().minusHours(1))
                .build();
        when(partidaRepository.bloquearPorId(200L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.impugnar(200L, ORGANIZADOR, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bye");
    }

    @Test
    void impugnar_deja_la_partida_en_disputa() {
        Partida p = partidaFinalizadaHaceHoras(1);
        service.impugnar(200L, CAPITAN_A, false, request());
        assertThat(p.getEstado()).isEqualTo(EstadoPartida.EN_DISPUTA);
    }

    @Test
    void no_se_puede_impugnar_si_el_torneo_ya_finalizo() {
        // Sin este corte, impugnar la final dentro del plazo dejaba la
        // disputa sin nadie que pudiera cerrarla: resolver exige que el
        // torneo siga EN_CURSO, pero el campeon ya estaba coronado.
        torneo.setEstado(EstadoTorneo.FINALIZADO);
        partidaFinalizadaHaceHoras(1);

        assertThatThrownBy(() -> service.impugnar(200L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en curso");
    }
}