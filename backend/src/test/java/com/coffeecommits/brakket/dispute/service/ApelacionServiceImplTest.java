package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.dispute.dto.ApelacionResponse;
import com.coffeecommits.brakket.dispute.dto.ApelarRequest;
import com.coffeecommits.brakket.dispute.dto.ResolverApelacionRequest;
import com.coffeecommits.brakket.dispute.model.Apelacion;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.repository.ApelacionRepository;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.service.PartidaService;
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
class ApelacionServiceImplTest {

    @Mock
    private ApelacionRepository apelacionRepository;
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
    @Mock
    private PartidaService partidaService;

    private ApelacionServiceImpl service;

    private static final String CAPITAN_A = "capa@brakket.gg";
    private static final String COMISIONADO = "comisionado@brakket.gg";
    private static final String ARBITRO_QUE_RESOLVIO = "arbitro@brakket.gg";
    private static final String AJENO = "ajeno@brakket.gg";

    private Usuario resueltaPor;
    private Disputa disputa;

    @BeforeEach
    void setUp() {
        DisputaGuard guard = new DisputaGuard(inscripcionRepository, arbitroTorneoRepository);
        service = new ApelacionServiceImpl(apelacionRepository, disputaRepository, partidaRepository,
                usuarioRepository, guard, partidaService);

        Usuario comisionado = Usuario.builder().id(5L).correo(COMISIONADO).build();
        Liga liga = Liga.builder().id(2L).comisionado(comisionado).build();
        Temporada temporada = Temporada.builder().id(3L).liga(liga).build();
        Torneo torneo = Torneo.builder().id(7L)
                .organizador(Usuario.builder().id(1L).correo("orga@brakket.gg").build())
                .temporada(temporada)
                .build();
        Equipo equipoA = Equipo.builder().id(10L).nombre("Azules").build();
        Equipo equipoB = Equipo.builder().id(20L).nombre("Rojos").build();
        Partida partida = Partida.builder().id(200L).torneo(torneo)
                .equipoA(equipoA).equipoB(equipoB)
                .estado(EstadoPartida.FINALIZADA)
                .build();

        resueltaPor = Usuario.builder().id(50L).correo(ARBITRO_QUE_RESOLVIO).build();
        disputa = Disputa.builder().id(100L).partida(partida)
                .levantadaPor(Usuario.builder().id(10L).correo(CAPITAN_A).build())
                .estado("RESUELTA")
                .resueltaPor(resueltaPor)
                .fechaResolucion(LocalDateTime.now().minusHours(1))
                .build();

        lenient().when(disputaRepository.findById(100L)).thenReturn(Optional.of(disputa));
        lenient().when(usuarioRepository.findByCorreo(CAPITAN_A))
                .thenReturn(Optional.of(Usuario.builder().id(10L).correo(CAPITAN_A).build()));
        lenient().when(usuarioRepository.findByCorreo(COMISIONADO)).thenReturn(Optional.of(comisionado));
        lenient().when(usuarioRepository.findByCorreo(ARBITRO_QUE_RESOLVIO)).thenReturn(Optional.of(resueltaPor));
        lenient().when(usuarioRepository.findByCorreo(AJENO))
                .thenReturn(Optional.of(Usuario.builder().id(99L).correo(AJENO).build()));
        lenient().when(inscripcionRepository.esCapitanActivo(10L, 10L)).thenReturn(true);
        lenient().when(apelacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(partidaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(disputaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ApelarRequest request() {
        return new ApelarRequest("No estoy de acuerdo con la resolución");
    }

    @Test
    void un_capitan_relacionado_puede_apelar_dentro_del_plazo() {
        ApelacionResponse resp = service.apelar(100L, CAPITAN_A, false, request());
        assertThat(resp.estado()).isEqualTo("PENDIENTE");
        assertThat(disputa.getEstado()).isEqualTo("EN_APELACION");
    }

    @Test
    void solo_se_puede_apelar_una_disputa_ya_resuelta() {
        disputa.setEstado("PENDIENTE");
        assertThatThrownBy(() -> service.apelar(100L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("resuelta");
    }

    @Test
    void quien_resolvio_la_disputa_no_puede_apelar_su_propia_decision() {
       
        when(arbitroTorneoRepository.findByTorneoId(7L)).thenReturn(List.of(
                com.coffeecommits.brakket.tournament.model.ArbitroTorneo.builder()
                        .id(1L).usuario(resueltaPor).build()));

        assertThatThrownBy(() -> service.apelar(100L, ARBITRO_QUE_RESOLVIO, false, request()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("propia decisión");
    }

    @Test
    void un_ajeno_a_la_partida_no_puede_apelar() {
        assertThatThrownBy(() -> service.apelar(100L, AJENO, false, request()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void fuera_del_plazo_de_48_horas_no_se_puede_apelar() {
        disputa.setFechaResolucion(LocalDateTime.now().minusHours(49));
        assertThatThrownBy(() -> service.apelar(100L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("plazo");
    }

    @Test
    void no_se_puede_apelar_dos_veces_la_misma_disputa() {
        when(apelacionRepository.findByDisputaId(100L)).thenReturn(List.of(
                Apelacion.builder().id(1L).disputa(disputa).estado("RESUELTA").build()));
        assertThatThrownBy(() -> service.apelar(100L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue apelada");
    }

    @Test
    void solo_el_comisionado_de_la_liga_o_un_admin_resuelven_la_apelacion() {
        Apelacion apelacion = Apelacion.builder().id(1L).disputa(disputa).estado("PENDIENTE").build();
        when(apelacionRepository.findById(1L)).thenReturn(Optional.of(apelacion));

        ResolverApelacionRequest req = new ResolverApelacionRequest("Confirmo la decisión", null);

        // El comisionado sí puede.
        ApelacionResponse resp = service.resolver(1L, COMISIONADO, false, req);
        assertThat(resp.estado()).isEqualTo("RESUELTA");
        assertThat(disputa.getEstado()).isEqualTo("RESUELTA");

        // Un capitán ajeno a la liga no puede.
        apelacion.setEstado("PENDIENTE");
        assertThatThrownBy(() -> service.resolver(1L, CAPITAN_A, false, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void no_se_puede_resolver_una_apelacion_ya_resuelta() {
        Apelacion apelacion = Apelacion.builder().id(1L).disputa(disputa).estado("RESUELTA").build();
        when(apelacionRepository.findById(1L)).thenReturn(Optional.of(apelacion));

        assertThatThrownBy(() -> service.resolver(1L, COMISIONADO, false,
                new ResolverApelacionRequest("otra vez", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue resuelta");
    }

    @Test
    void listar_por_disputa_exige_estar_relacionado_con_la_partida() {
        when(apelacionRepository.findByDisputaId(100L)).thenReturn(List.of(
                Apelacion.builder().id(1L).disputa(disputa).estado("RESUELTA").build()));

        List<ApelacionResponse> lista = service.listarPorDisputa(100L, CAPITAN_A, false);
        assertThat(lista).hasSize(1);

        assertThatThrownBy(() -> service.listarPorDisputa(100L, AJENO, false))
                .isInstanceOf(ForbiddenException.class);
    }
}