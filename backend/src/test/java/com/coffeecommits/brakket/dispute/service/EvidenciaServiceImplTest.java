package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.dispute.dto.AdjuntarEvidenciaRequest;
import com.coffeecommits.brakket.dispute.dto.EvidenciaResponse;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.model.EvidenciaDisputa;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.dispute.repository.EvidenciaDisputaRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.model.ArbitroTorneo;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
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
class EvidenciaServiceImplTest {

    @Mock
    private DisputaRepository disputaRepository;
    @Mock
    private EvidenciaDisputaRepository evidenciaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private ArbitroTorneoRepository arbitroTorneoRepository;

    private EvidenciaServiceImpl service;

    private static final String ORGANIZADOR = "orga@brakket.gg";
    private static final String CAPITAN_A = "capa@brakket.gg";
    private static final String CAPITAN_B = "capb@brakket.gg";
    private static final String ARBITRO = "arbitro@brakket.gg";
    private static final String AJENO = "ajeno@brakket.gg";

    private Torneo torneo;
    private Disputa disputa;

    @BeforeEach
    void setUp() {
        DisputaGuard guard = new DisputaGuard(inscripcionRepository, arbitroTorneoRepository);
        service = new EvidenciaServiceImpl(disputaRepository, evidenciaRepository, usuarioRepository, guard);

        Equipo equipoA = Equipo.builder().id(10L).nombre("Azules").build();
        Equipo equipoB = Equipo.builder().id(20L).nombre("Rojos").build();
        torneo = Torneo.builder().id(7L)
                .organizador(Usuario.builder().id(1L).correo(ORGANIZADOR).build())
                .estado(EstadoTorneo.FINALIZADO)
                .build();
        Partida partida = Partida.builder().id(200L).torneo(torneo).equipoA(equipoA).equipoB(equipoB).build();
        disputa = Disputa.builder().id(5L).partida(partida)
                .levantadaPor(Usuario.builder().id(10L).correo(CAPITAN_A).build())
                .estado("PENDIENTE")
                .build();

        lenient().when(disputaRepository.findById(5L)).thenReturn(Optional.of(disputa));
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
        lenient().when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AdjuntarEvidenciaRequest request() {
        return new AdjuntarEvidenciaRequest("https://ejemplo.com/captura.png", "captura del marcador");
    }

    @Test
    void capitanes_organizador_arbitro_y_admin_pueden_adjuntar_pero_un_ajeno_no() {
        Usuario arbitroUsuario = Usuario.builder().id(50L).correo(ARBITRO).build();
        when(arbitroTorneoRepository.findByTorneoId(7L)).thenReturn(List.of(
                ArbitroTorneo.builder().id(1L).torneo(torneo).usuario(arbitroUsuario).build()));

        assertThat(service.adjuntar(5L, CAPITAN_A, false, request()).url())
                .isEqualTo("https://ejemplo.com/captura.png");
        assertThat(service.adjuntar(5L, CAPITAN_B, false, request()).url()).isNotNull();
        assertThat(service.adjuntar(5L, ORGANIZADOR, false, request()).url()).isNotNull();
        assertThat(service.adjuntar(5L, ARBITRO, false, request()).url()).isNotNull();
        assertThat(service.adjuntar(5L, AJENO, true, request()).url()).isNotNull(); // admin

        assertThatThrownBy(() -> service.adjuntar(5L, AJENO, false, request()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void no_se_puede_adjuntar_evidencia_a_una_disputa_ya_cerrada() {
        disputa.setEstado("RESUELTA");
        assertThatThrownBy(() -> service.adjuntar(5L, CAPITAN_A, false, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("abierta");
    }

    @Test
    void listar_exige_el_mismo_permiso_que_adjuntar() {
        EvidenciaDisputa e = EvidenciaDisputa.builder().id(1L).disputa(disputa)
                .subidoPor(disputa.getLevantadaPor())
                .url("https://ejemplo.com/e.png")
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(evidenciaRepository.findByDisputaIdOrderByFechaCreacionAsc(5L)).thenReturn(List.of(e));

        List<EvidenciaResponse> lista = service.listar(5L, CAPITAN_A, false);
        assertThat(lista).hasSize(1);

        assertThatThrownBy(() -> service.listar(5L, AJENO, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listar_no_exige_disputa_activa_solo_relacion_con_la_partida() {
        disputa.setEstado("RESUELTA");
        when(evidenciaRepository.findByDisputaIdOrderByFechaCreacionAsc(5L)).thenReturn(List.of());
        assertThat(service.listar(5L, CAPITAN_A, false)).isEmpty();
    }
}