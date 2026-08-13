package com.coffeecommits.brakket.report.service;

import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.service.AuthService;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.dispute.model.Disputa;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.report.dto.FiltrosReporteRequest;
import com.coffeecommits.brakket.report.dto.ReporteResponse;
import com.coffeecommits.brakket.report.model.ReporteGenerado;
import com.coffeecommits.brakket.report.model.TipoReporte;
import com.coffeecommits.brakket.report.repository.ReporteGeneradoRepository;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.statistics.model.EstadisticaJugador;
import com.coffeecommits.brakket.statistics.repository.EstadisticaJugadorRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import com.coffeecommits.brakket.game.model.Juego;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock private AuthService authService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PatrocinadorRepository patrocinadorRepository;
    @Mock private PatrocinioRepository patrocinioRepository;
    @Mock private TorneoRepository torneoRepository;
    @Mock private PartidaRepository partidaRepository;
    @Mock private DisputaRepository disputaRepository;
    @Mock private TransmisionTwitchRepository transmisionRepository;
    @Mock private MetricaAudienciaRepository metricaAudienciaRepository;
    @Mock private MetricaChatRepository metricaChatRepository;
    @Mock private AnalisisSentimientoRepository sentimientoRepository;
    @Mock private EstadisticaJugadorRepository estadisticaJugadorRepository;
    @Mock private ReporteGeneradoRepository reporteGeneradoRepository;

    @Mock private Authentication authentication;
    @Mock private UsuarioResponse usuarioResponse;

    private ReporteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReporteServiceImpl(authService, usuarioRepository, patrocinadorRepository,
                patrocinioRepository, torneoRepository, partidaRepository, disputaRepository,
                transmisionRepository, metricaAudienciaRepository, metricaChatRepository,
                sentimientoRepository, estadisticaJugadorRepository, reporteGeneradoRepository);
    }

    private void mockUsuarioActual(Long id, String nombre) {
        // authentication.getName() no estaba stubbeado en los tests de ADMIN, así
        // que devolvía null por defecto, y anyString() no hace match con null —
        // se stubbea acá para que todos los tests que usan este helper queden cubiertos.
        when(authentication.getName()).thenReturn("usuario-test@demo.com");
        when(authService.getCurrentUser(anyString())).thenReturn(usuarioResponse);
        when(usuarioResponse.id()).thenReturn(id);
        when(usuarioResponse.nombre()).thenReturn(nombre);
        when(usuarioRepository.getReferenceById(id)).thenReturn(mock(Usuario.class));
    }

    // doReturn(...).when(...) en vez de when(...).thenReturn(...): getAuthorities()
    // devuelve Collection<? extends GrantedAuthority>, y con thenReturn el compilador
    // no logra inferir el tipo del wildcard a partir de List<SimpleGrantedAuthority>.
    private void mockRol(String rol) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        doReturn(authorities).when(authentication).getAuthorities();
    }

    @Test
    void generar_estadistica_ignora_filtros_y_devuelve_el_acumulado_completo() {
        mockRol("ADMIN");
        mockUsuarioActual(1L, "Admin Uno");

        Usuario jugador = mock(Usuario.class);
        when(jugador.getNombre()).thenReturn("Jugador X");
        Juego juego = mock(Juego.class);
        when(juego.getNombre()).thenReturn("Rocket League");

        EstadisticaJugador estadistica = EstadisticaJugador.builder()
                .usuario(jugador).juego(juego).victorias(10).derrotas(4).torneosJugados(3).build();
        when(estadisticaJugadorRepository.findAll()).thenReturn(List.of(estadistica));

        FiltrosReporteRequest filtros = new FiltrosReporteRequest(99L, 99L, LocalDate.now(), LocalDate.now());
        ReporteResponse reporte = service.generar(TipoReporte.ESTADISTICA, filtros, authentication);

        assertThat(reporte.filas()).hasSize(1);
        assertThat(reporte.filas().get(0)).containsExactly("Jugador X", "Rocket League", "10", "4", "3");
        // Prueba explícita de la Opción A: aunque el filtro traiga torneoId/patrocinadorId/fechas, se ignoran.
        verifyNoInteractions(torneoRepository, patrocinioRepository);
        verify(reporteGeneradoRepository).save(any(ReporteGenerado.class));
    }

    @Test
    void generar_patrocinio_usa_buscarParaReporte_y_mapea_las_filas() {
        mockRol("ADMIN");
        mockUsuarioActual(1L, "Admin Uno");

        Patrocinador patrocinador = mock(Patrocinador.class);
        when(patrocinador.getNombre()).thenReturn("Nike Demo");
        Torneo torneo = mock(Torneo.class);
        when(torneo.getNombre()).thenReturn("Copa Relampago");

        Patrocinio patrocinio = mock(Patrocinio.class);
        when(patrocinio.getPatrocinador()).thenReturn(patrocinador);
        when(patrocinio.getTorneo()).thenReturn(torneo);
        when(patrocinio.getNivel()).thenReturn("ORO");
        when(patrocinio.getEstado()).thenReturn("ACTIVO");
        when(patrocinio.getFechaInicio()).thenReturn(LocalDate.of(2026, 1, 1));
        when(patrocinio.getFechaFin()).thenReturn(LocalDate.of(2026, 12, 31));

        when(patrocinioRepository.buscarParaReporte(5L, null, null, null))
                .thenReturn(List.of(patrocinio));

        FiltrosReporteRequest filtros = new FiltrosReporteRequest(5L, null, null, null);
        ReporteResponse reporte = service.generar(TipoReporte.PATROCINIO, filtros, authentication);

        assertThat(reporte.filas()).containsExactly(
                List.of("Nike Demo", "Torneo: Copa Relampago", "ORO", "ACTIVO", "2026-01-01", "2026-12-31"));
    }

    @Test
    void un_patrocinador_no_puede_ver_datos_de_otra_marca_aunque_los_pida() {
        mockRol("PATROCINADOR");
        mockUsuarioActual(42L, "Marca Propia");

        Patrocinador propio = mock(Patrocinador.class);
        when(propio.getId()).thenReturn(7L);
        when(patrocinadorRepository.findByUsuarioId(42L)).thenReturn(Optional.of(propio));
        when(patrocinioRepository.buscarParaReporte(isNull(), eq(7L), isNull(), isNull()))
                .thenReturn(List.of());

        // Pide explícitamente los datos de OTRA marca (patrocinadorId=999).
        FiltrosReporteRequest filtros = new FiltrosReporteRequest(null, 999L, null, null);
        service.generar(TipoReporte.PATROCINIO, filtros, authentication);

        // El id ajeno nunca llega al repositorio: se sustituye por el propio (7L).
        verify(patrocinioRepository).buscarParaReporte(isNull(), eq(7L), isNull(), isNull());
        verify(patrocinioRepository, never()).buscarParaReporte(isNull(), eq(999L), isNull(), isNull());
    }

    @Test
    void un_patrocinador_sin_perfil_vinculado_recibe_forbidden() {
        mockRol("PATROCINADOR");
        when(authentication.getName()).thenReturn("sinperfil@demo.com");
        when(authService.getCurrentUser("sinperfil@demo.com")).thenReturn(usuarioResponse);
        when(usuarioResponse.id()).thenReturn(55L);
        when(patrocinadorRepository.findByUsuarioId(55L)).thenReturn(Optional.empty());

        FiltrosReporteRequest filtros = new FiltrosReporteRequest(null, null, null, null);

        assertThatThrownBy(() -> service.generar(TipoReporte.PATROCINIO, filtros, authentication))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void generar_competencia_incluye_la_disputa_resuelta_de_cada_partida() {
        mockRol("ADMIN");
        mockUsuarioActual(1L, "Admin Uno");

        Juego juego = mock(Juego.class);
        when(juego.getNombre()).thenReturn("League of Legends");
        Torneo torneo = mock(Torneo.class);
        when(torneo.getId()).thenReturn(1L);
        when(torneo.getNombre()).thenReturn("Copa Relampago");
        when(torneo.getJuego()).thenReturn(juego);
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));

        Equipo equipoA = mock(Equipo.class);
        when(equipoA.getNombre()).thenReturn("Fenix Demo");
        Equipo equipoB = mock(Equipo.class);
        when(equipoB.getNombre()).thenReturn("Lobos Demo");

        Partida partida = mock(Partida.class);
        when(partida.getId()).thenReturn(100L);
        when(partida.getRonda()).thenReturn(1);
        when(partida.getEquipoA()).thenReturn(equipoA);
        when(partida.getEquipoB()).thenReturn(equipoB);
        when(partida.getMarcadorA()).thenReturn(3);
        when(partida.getMarcadorB()).thenReturn(1);
        when(partida.getGanador()).thenReturn(equipoA);
        when(partida.getEstado()).thenReturn(EstadoPartida.FINALIZADA);
        when(partidaRepository.findByTorneoIdOrderByRondaAscOrdenAsc(1L)).thenReturn(List.of(partida));

        Disputa disputa = mock(Disputa.class);
        when(disputa.getFechaResolucion()).thenReturn(LocalDateTime.now());
        when(disputa.getDecision()).thenReturn("MANTENER");
        when(disputaRepository.findByPartidaId(100L)).thenReturn(List.of(disputa));

        FiltrosReporteRequest filtros = new FiltrosReporteRequest(1L, null, null, null);
        ReporteResponse reporte = service.generar(TipoReporte.COMPETENCIA, filtros, authentication);

        List<String> fila = reporte.filas().get(0);
        assertThat(fila.get(fila.size() - 1)).isEqualTo("Resuelta: MANTENER");
    }
}