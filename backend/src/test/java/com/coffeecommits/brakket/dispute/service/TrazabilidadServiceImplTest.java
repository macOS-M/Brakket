package com.coffeecommits.brakket.dispute.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.dispute.model.Apelacion;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.model.EvidenciaDisputa;
import com.coffeecommits.brakket.dispute.repository.ApelacionRepository;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.dispute.repository.EvidenciaDisputaRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.dto.EventoTrazabilidadResponse;
import com.coffeecommits.brakket.tournament.model.CasoEspecialPartida;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.TipoCasoEspecial;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.ArbitroTorneoRepository;
import com.coffeecommits.brakket.tournament.repository.CasoEspecialPartidaRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrazabilidadServiceImplTest {

    @Mock
    private PartidaRepository partidaRepository;
    @Mock
    private CasoEspecialPartidaRepository casoEspecialPartidaRepository;
    @Mock
    private DisputaRepository disputaRepository;
    @Mock
    private EvidenciaDisputaRepository evidenciaDisputaRepository;
    @Mock
    private ApelacionRepository apelacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private ArbitroTorneoRepository arbitroTorneoRepository;

    private TrazabilidadServiceImpl service;

    private static final String CAPITAN_A = "capa@brakket.gg";
    private static final String AJENO = "ajeno@brakket.gg";

    private Partida partida;

    @BeforeEach
    void setUp() {
        DisputaGuard guard = new DisputaGuard(inscripcionRepository, arbitroTorneoRepository);
        service = new TrazabilidadServiceImpl(partidaRepository, casoEspecialPartidaRepository,
                disputaRepository, evidenciaDisputaRepository, apelacionRepository, usuarioRepository, guard);

        Equipo equipoA = Equipo.builder().id(10L).nombre("Azules").build();
        Equipo equipoB = Equipo.builder().id(20L).nombre("Rojos").build();
        Torneo torneo = Torneo.builder().id(7L)
                .organizador(Usuario.builder().id(1L).correo("orga@brakket.gg").build())
                .build();
        partida = Partida.builder().id(200L).torneo(torneo)
                .equipoA(equipoA).equipoB(equipoB)
                .ganador(equipoA)
                .marcadorA(3).marcadorB(1)
                .fechaFinalizacion(LocalDateTime.now().minusHours(3))
                .build();

        lenient().when(partidaRepository.findById(200L)).thenReturn(Optional.of(partida));
        lenient().when(usuarioRepository.findByCorreo(CAPITAN_A))
                .thenReturn(Optional.of(Usuario.builder().id(10L).correo(CAPITAN_A).build()));
        lenient().when(usuarioRepository.findByCorreo(AJENO))
                .thenReturn(Optional.of(Usuario.builder().id(99L).correo(AJENO).build()));
        lenient().when(inscripcionRepository.esCapitanActivo(10L, 10L)).thenReturn(true);
        lenient().when(casoEspecialPartidaRepository.findByPartidaIdOrderByFechaDesc(200L))
                .thenReturn(List.of());
        lenient().when(disputaRepository.findByPartidaId(200L)).thenReturn(List.of());
    }

    @Test
    void un_ajeno_a_la_partida_no_puede_ver_la_trazabilidad() {
        assertThatThrownBy(() -> service.obtener(200L, AJENO, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void junta_eventos_de_varias_fuentes_y_los_ordena_por_fecha() {
        LocalDateTime haceTresHoras = partida.getFechaFinalizacion();
        LocalDateTime haceDosHoras = haceTresHoras.plusHours(1);
        LocalDateTime haceUnaHora = haceTresHoras.plusHours(2);

        // Un caso especial más viejo que el resultado (para mezclar el orden).
        Usuario registrador = Usuario.builder().id(1L).nombre("Olivia").correo("orga@brakket.gg").build();
        when(casoEspecialPartidaRepository.findByPartidaIdOrderByFechaDesc(200L)).thenReturn(List.of(
                CasoEspecialPartida.builder().id(1L).partida(partida)
                        .tipo(TipoCasoEspecial.DESCANSO)
                        .registradoPor(registrador)
                        .fecha(haceTresHoras.minusMinutes(30))
                        .build()));

        // Una disputa impugnada después del resultado, y resuelta más tarde.
        Usuario capitan = Usuario.builder().id(10L).nombre("Ana").correo(CAPITAN_A).build();
        Disputa disputa = Disputa.builder().id(5L).partida(partida)
                .levantadaPor(capitan)
                .motivo("motivo").descripcion("descripcion")
                .estado("RESUELTA")
                .fechaCreacion(haceDosHoras)
                .decision("MANTENER").justificacionResolucion("justif")
                .resueltaPor(registrador)
                .fechaResolucion(haceUnaHora)
                .build();
        when(disputaRepository.findByPartidaId(200L)).thenReturn(List.of(disputa));
        when(evidenciaDisputaRepository.findByDisputaIdOrderByFechaCreacionAsc(5L)).thenReturn(List.of(
                EvidenciaDisputa.builder().id(1L).disputa(disputa)
                        .subidoPor(capitan).url("http://x.com/foto.png")
                        .fechaCreacion(haceDosHoras.plusMinutes(10))
                        .build()));
        when(apelacionRepository.findByDisputaId(5L)).thenReturn(List.of());

        List<EventoTrazabilidadResponse> eventos = service.obtener(200L, CAPITAN_A, false);

        assertThat(eventos).hasSize(5);
        // Deben venir ordenados de más viejo a más nuevo, sin importar de
        // qué fuente vino cada uno (caso especial, resultado, disputa, evidencia).
        assertThat(eventos).extracting(EventoTrazabilidadResponse::tipo)
                .containsExactly("CASO_ESPECIAL_DESCANSO", "RESULTADO", "IMPUGNACION", "EVIDENCIA", "RESOLUCION_DISPUTA");
        assertThat(eventos).isSortedAccordingTo(
                (a, b) -> a.fecha().compareTo(b.fecha()));
    }
}