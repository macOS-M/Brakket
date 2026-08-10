package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.model.ModalidadCompetitiva;
import com.coffeecommits.brakket.game.model.PerfilCompetitivoJuego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.game.repository.PerfilCompetitivoRepository;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.dto.CrearTorneoRequest;
import com.coffeecommits.brakket.tournament.dto.TorneoDetalleResponse;
import com.coffeecommits.brakket.tournament.dto.TorneoResponse;
import com.coffeecommits.brakket.tournament.model.Inscripcion;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorneoServiceImplTest {

    @Mock
    private TorneoRepository torneoRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private JuegoRepository juegoRepository;
    @Mock
    private TemporadaRepository temporadaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCompetitivoRepository perfilCompetitivoRepository;
    @InjectMocks
    private TorneoServiceImpl torneoService;

    private static final String CORREO = "ana@brakket.gg";
    private static final LocalDateTime FUTURO = LocalDateTime.now().plusDays(3);

    private Usuario usuario() {
        return Usuario.builder().id(1L).nombre("Ana").correo(CORREO).build();
    }

    private Juego juego() {
        return Juego.builder().id(3L).nombre("Valorant").genero("Shooter").activo(true).build();
    }

    private CrearTorneoRequest request(Long temporadaId, LocalDateTime fecha) {
        return new CrearTorneoRequest(
                "Copa Nocturna", 3L, temporadaId, "Eliminación directa", 5, 8, fecha, true, null, null, null);
    }

    private Torneo torneoAbierto() {
        return Torneo.builder()
                .id(7L).juego(juego()).organizador(usuario())
                .nombre("Copa Nocturna").formato("Eliminación directa")
                .tamanoEquipo(5).maxEquipos(8)
                .fechaInicio(FUTURO).estado(EstadoTorneo.INSCRIPCION_ABIERTA).publico(true)
                .build();
    }

    @Test
    void crear_torneo_comunitario_sin_temporada_queda_abierto() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(juegoRepository.findById(3L)).thenReturn(Optional.of(juego()));
        when(perfilCompetitivoRepository.findByJuegoId(3L)).thenReturn(Optional.empty());
        when(torneoRepository.save(any(Torneo.class))).thenAnswer(inv -> {
            Torneo t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TorneoResponse resp = torneoService.crearTorneo(CORREO, false, request(null, FUTURO));

        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.estado()).isEqualTo("INSCRIPCION_ABIERTA");
        assertThat(resp.ligaId()).isNull();
        assertThat(resp.organizadorNombre()).isEqualTo("Ana");
    }

    @Test
    void crear_rechaza_fecha_de_inicio_pasada() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(juegoRepository.findById(3L)).thenReturn(Optional.of(juego()));

        assertThatThrownBy(() -> torneoService.crearTorneo(
                CORREO, false, request(null, LocalDateTime.now().minusHours(1))))
                .isInstanceOf(BusinessException.class);
        verify(torneoRepository, never()).save(any());
    }

    @Test
    void crear_respeta_los_limites_del_perfil_competitivo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(juegoRepository.findById(3L)).thenReturn(Optional.of(juego()));
        when(perfilCompetitivoRepository.findByJuegoId(3L)).thenReturn(Optional.of(
                PerfilCompetitivoJuego.builder()
                        .juego(juego()).modalidad(ModalidadCompetitiva.EQUIPOS)
                        .plantillaMinima(2).plantillaMaxima(3).activo(true)
                        .build()));

        // El request pide 5v5 pero el perfil permite 2 a 3.
        assertThatThrownBy(() -> torneoService.crearTorneo(CORREO, false, request(null, FUTURO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 a 3");
    }

    @Test
    void crear_en_temporada_ajena_exige_ser_comisionado_de_la_liga() {
        Usuario otroDueno = Usuario.builder().id(9L).nombre("Beto").correo("beto@x.com").build();
        Temporada temporada = Temporada.builder().id(4L)
                .liga(Liga.builder().id(2L).nombre("Liga X").juego(juego()).comisionado(otroDueno).build())
                .nombre("T1").build();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(juegoRepository.findById(3L)).thenReturn(Optional.of(juego()));
        when(perfilCompetitivoRepository.findByJuegoId(3L)).thenReturn(Optional.empty());
        when(temporadaRepository.findById(4L)).thenReturn(Optional.of(temporada));

        assertThatThrownBy(() -> torneoService.crearTorneo(CORREO, false, request(4L, FUTURO)))
                .isInstanceOf(ForbiddenException.class);

        // Un ADMIN sí puede hospedarlo en una liga ajena.
        when(torneoRepository.save(any(Torneo.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(torneoService.crearTorneo(CORREO, true, request(4L, FUTURO)).ligaNombre())
                .isEqualTo("Liga X");
    }

    @Test
    void inscribir_valida_capitania_cupo_y_duplicados() {
        Torneo torneo = torneoAbierto();
        Equipo equipo = Equipo.builder().id(20L).nombre("Nébula").juego(juego()).build();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(torneoRepository.findById(7L)).thenReturn(Optional.of(torneo));
        lenient().when(inscripcionRepository.countVigentesPorTorneo(7L)).thenReturn(0L);

        // No es capitán → prohibido.
        when(inscripcionRepository.esCapitanActivo(1L, 20L)).thenReturn(false);
        assertThatThrownBy(() -> torneoService.inscribirEquipo(7L, CORREO, 20L, "AnaRL"))
                .isInstanceOf(ForbiddenException.class);

        // Capitán con plantilla suficiente → inscribe.
        when(inscripcionRepository.esCapitanActivo(1L, 20L)).thenReturn(true);
        when(inscripcionRepository.existsByTorneoIdAndEquipoId(7L, 20L)).thenReturn(false);
        when(inscripcionRepository.equiposCapitaneadosPor(1L)).thenReturn(List.of(equipo));
        when(inscripcionRepository.countMiembrosActivos(20L)).thenReturn(5L);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionRepository.findByTorneoId(7L)).thenReturn(List.of());

        TorneoDetalleResponse detalle = torneoService.inscribirEquipo(7L, CORREO, 20L, " AnaRL ");
        assertThat(detalle.torneo().id()).isEqualTo(7L);
        // El gamertag viaja recortado en la inscripción (identidad en el juego).
        verify(inscripcionRepository).save(argThat(i -> "AnaRL".equals(i.getUsuarioEnJuego())));
    }

    @Test
    void inscribir_rechaza_cupo_lleno_y_plantilla_corta() {
        Torneo torneo = torneoAbierto();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(torneoRepository.findById(7L)).thenReturn(Optional.of(torneo));

        // Cupo lleno.
        when(inscripcionRepository.countVigentesPorTorneo(7L)).thenReturn(8L);
        assertThatThrownBy(() -> torneoService.inscribirEquipo(7L, CORREO, 20L, "AnaRL"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cupo");

        // Plantilla corta para un 5v5.
        when(inscripcionRepository.countVigentesPorTorneo(7L)).thenReturn(0L);
        when(inscripcionRepository.esCapitanActivo(1L, 20L)).thenReturn(true);
        when(inscripcionRepository.existsByTorneoIdAndEquipoId(7L, 20L)).thenReturn(false);
        when(inscripcionRepository.equiposCapitaneadosPor(1L)).thenReturn(List.of(
                Equipo.builder().id(20L).nombre("Nébula").juego(juego()).build()));
        when(inscripcionRepository.countMiembrosActivos(20L)).thenReturn(2L);
        assertThatThrownBy(() -> torneoService.inscribirEquipo(7L, CORREO, 20L, "AnaRL"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 jugador");
    }

    @Test
    void un_torneo_privado_no_existe_para_terceros() {
        Torneo privado = torneoAbierto();
        privado.setPublico(false);
        when(torneoRepository.findById(7L)).thenReturn(Optional.of(privado));
        when(usuarioRepository.findByCorreo("tercero@x.com")).thenReturn(
                Optional.of(Usuario.builder().id(99L).nombre("Tercero").correo("tercero@x.com").build()));

        assertThatThrownBy(() -> torneoService.obtenerDetalle(7L, "tercero@x.com", false))
                .isInstanceOf(com.coffeecommits.brakket.common.exception.ResourceNotFoundException.class);

        // Su organizador sí lo ve.
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(inscripcionRepository.findByTorneoId(7L)).thenReturn(List.of());
        assertThat(torneoService.obtenerDetalle(7L, CORREO, false).torneo().publico()).isFalse();
    }

    @Test
    void eliminar_solo_organizador_o_admin() {
        Torneo torneo = torneoAbierto();
        when(torneoRepository.findById(7L)).thenReturn(Optional.of(torneo));
        when(usuarioRepository.findByCorreo("tercero@x.com")).thenReturn(
                Optional.of(Usuario.builder().id(99L).nombre("Tercero").correo("tercero@x.com").build()));

        assertThatThrownBy(() -> torneoService.eliminarTorneo(7L, "tercero@x.com", false))
                .isInstanceOf(ForbiddenException.class);

        torneoService.eliminarTorneo(7L, "tercero@x.com", true);
        verify(torneoRepository).delete(torneo);
    }

    @Test
    void eliminar_un_torneo_en_curso_esta_bloqueado() {
        Torneo torneo = torneoAbierto();
        torneo.setEstado(EstadoTorneo.EN_CURSO);
        when(torneoRepository.findById(7L)).thenReturn(Optional.of(torneo));

        assertThatThrownBy(() -> torneoService.eliminarTorneo(7L, "tercero@x.com", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en curso");
        verify(torneoRepository, never()).delete(torneo);
    }
}
