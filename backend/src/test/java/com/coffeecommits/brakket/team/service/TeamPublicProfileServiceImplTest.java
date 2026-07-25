package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.statistics.model.EstadisticaJugador;
import com.coffeecommits.brakket.statistics.repository.EstadisticaJugadorRepository;
import com.coffeecommits.brakket.team.dto.EquipoResumenPublicoResponse;
import com.coffeecommits.brakket.team.dto.PerfilEquipoPublicoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRedSocialRepository;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class TeamPublicProfileServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private MiembroEquipoRepository miembroRepository;
    @Mock
    private EquipoRedSocialRepository redSocialRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private EstadisticaJugadorRepository estadisticaRepository;
    @Mock
    private com.coffeecommits.brakket.tournament.repository.PartidaRepository partidaRepository;
    @InjectMocks
    private TeamPublicProfileServiceImpl service;

    private Usuario jugador(Long id, String nombre) {
        return Usuario.builder().id(id).nombre(nombre).correo(nombre + "@brakket.gg").build();
    }

    private Equipo equipo() {
        return Equipo.builder()
                .id(10L).nombre("Los Invencibles").capitan(jugador(1L, "Capi"))
                .juego(Juego.builder().id(3L).nombre("Valorant").genero("FPS").build())
                .estado("ACTIVO")
                .build();
    }

    private MiembroEquipo miembro(Long usuarioId, String nombre, String rol) {
        return MiembroEquipo.builder()
                .usuario(jugador(usuarioId, nombre)).rol(rol)
                .estado("ACTIVO").fechaUnion(LocalDate.of(2026, 1, 15))
                .build();
    }

    @Test
    void consultarPerfil_agrega_estadisticas_de_la_plantilla() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo()));
        when(miembroRepository.findByEquipoIdAndEstado(10L, "ACTIVO"))
                .thenReturn(List.of(miembro(1L, "Capi", "CAPITAN"), miembro(2L, "Meta", "JUGADOR")));
        when(inscripcionRepository.findByEquipoId(10L)).thenReturn(List.of());
        when(redSocialRepository.findByEquipoId(10L)).thenReturn(List.of());
        when(estadisticaRepository.findByUsuarioIdAndJuegoId(1L, 3L)).thenReturn(
                Optional.of(EstadisticaJugador.builder().victorias(7).derrotas(3).torneosJugados(2).build()));
        when(estadisticaRepository.findByUsuarioIdAndJuegoId(2L, 3L)).thenReturn(
                Optional.of(EstadisticaJugador.builder().victorias(5).derrotas(5).torneosJugados(1).build()));

        PerfilEquipoPublicoResponse perfil = service.consultarPerfil(10L, null);

        assertThat(perfil.plantilla()).hasSize(2);
        assertThat(perfil.estadisticas().victorias()).isEqualTo(12);
        assertThat(perfil.estadisticas().derrotas()).isEqualTo(8);
        assertThat(perfil.estadisticas().torneosJugados()).isEqualTo(3);
        assertThat(perfil.estadisticas().disponibles()).isTrue();
    }

    @Test
    void consultarPerfil_prefiere_las_estadisticas_reales_del_motor_de_torneos() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo()));
        when(miembroRepository.findByEquipoIdAndEstado(10L, "ACTIVO"))
                .thenReturn(List.of(miembro(1L, "Capi", "CAPITAN")));
        when(inscripcionRepository.findByEquipoId(10L)).thenReturn(List.of());
        when(redSocialRepository.findByEquipoId(10L)).thenReturn(List.of());
        // El equipo ya jugó dentro de la plataforma: mandan las partidas
        // reales y las estadísticas manuales por jugador ni se consultan.
        when(partidaRepository.victoriasDe(10L)).thenReturn(5L);
        when(partidaRepository.derrotasDe(10L)).thenReturn(2L);

        PerfilEquipoPublicoResponse perfil = service.consultarPerfil(10L, null);

        assertThat(perfil.estadisticas().victorias()).isEqualTo(5);
        assertThat(perfil.estadisticas().derrotas()).isEqualTo(2);
        assertThat(perfil.estadisticas().disponibles()).isTrue();
        verify(estadisticaRepository, never()).findByUsuarioIdAndJuegoId(anyLong(), anyLong());
    }

    @Test
    void consultarPerfil_lanza_404_si_el_equipo_no_existe() {
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultarPerfil(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void consultarPerfil_sin_juego_no_consulta_estadisticas() {
        Equipo sinJuego = equipo();
        sinJuego.setJuego(null);
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(sinJuego));
        when(miembroRepository.findByEquipoIdAndEstado(10L, "ACTIVO"))
                .thenReturn(List.of(miembro(1L, "Capi", "CAPITAN")));
        when(inscripcionRepository.findByEquipoId(10L)).thenReturn(List.of());
        when(redSocialRepository.findByEquipoId(10L)).thenReturn(List.of());

        PerfilEquipoPublicoResponse perfil = service.consultarPerfil(10L, null);

        assertThat(perfil.estadisticas().disponibles()).isFalse();
        verify(estadisticaRepository, never()).findByUsuarioIdAndJuegoId(anyLong(), anyLong());
    }

    @Test
    void consultarPerfil_muestra_el_equipo_disuelto_como_historico() {
        Equipo disuelto = equipo();
        disuelto.setEstado("DISUELTO");
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(disuelto));
        when(miembroRepository.findByEquipoIdAndEstado(10L, "ACTIVO")).thenReturn(List.of());
        when(inscripcionRepository.findByEquipoId(10L)).thenReturn(List.of());
        when(redSocialRepository.findByEquipoId(10L)).thenReturn(List.of());

        PerfilEquipoPublicoResponse perfil = service.consultarPerfil(10L, null);

        assertThat(perfil.estado()).isEqualTo("DISUELTO");
    }

    @Test
    void buscarEquipos_devuelve_resumen_liviano_sin_armar_perfiles() {
        Equipo equipo = equipo();
        when(equipoRepository.buscarPorNombreConJuego("inven")).thenReturn(List.of(equipo));
        when(miembroRepository.contarActivosPorEquipo(List.of(10L)))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 5L}));

        List<EquipoResumenPublicoResponse> resultado = service.buscarEquipos("  inven  ");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Los Invencibles");
        assertThat(resultado.get(0).juegoNombre()).isEqualTo("Valorant");
        assertThat(resultado.get(0).integrantesActivos()).isEqualTo(5L);
        verify(miembroRepository, never()).findByEquipoIdAndEstado(anyLong(), any());
        verify(estadisticaRepository, never()).findByUsuarioIdAndJuegoId(anyLong(), anyLong());
    }

    @Test
    void buscarEquipos_sin_resultados_no_consulta_conteos() {
        when(equipoRepository.buscarPorNombreConJuego("nadie")).thenReturn(List.of());

        assertThat(service.buscarEquipos("nadie")).isEmpty();
        verify(miembroRepository, never()).contarActivosPorEquipo(any());
    }
}
