package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDissolutionServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PartidaRepository partidaRepository;
    @InjectMocks
    private TeamDissolutionServiceImpl service;

    private final Usuario capitan = Usuario.builder().id(1L).correo("cap@x.com").nombre("Capi").build();
    private final Usuario jugador = Usuario.builder().id(2L).correo("jug@x.com").nombre("Juga").build();

    private Equipo equipoActivo() {
        return Equipo.builder().id(10L).nombre("Coffee&Commits").capitan(capitan).estado("ACTIVO").build();
    }

    private MiembroEquipo miembro(Usuario usuario, String rol, String estado) {
        return MiembroEquipo.builder()
                .id(usuario.getId() + 100).usuario(usuario).estado(estado).rol(rol).build();
    }

    private void solicitanteEsCapitanActivo() {
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L))
                .thenReturn(Optional.of(miembro(capitan, "CAPITAN", "ACTIVO")));
    }

    @Test
    void disolver_marca_el_equipo_como_disuelto_con_fecha_responsable_y_motivo() {
        Equipo equipo = equipoActivo();
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(false);
        when(partidaRepository.existsPartidaPendientePorEquipo(10L)).thenReturn(false);
        when(equipoRepository.save(equipo)).thenReturn(equipo);

        EquipoResponse resp = service.disolver(10L,
                new DisolverEquipoRequest(true, "  Nos retiramos de la liga  "), "cap@x.com");

        assertThat(equipo.getEstado()).isEqualTo("DISUELTO");
        assertThat(equipo.getFechaDisolucion()).isNotNull();
        assertThat(equipo.getDisueltoPor()).isEqualTo(capitan);
        assertThat(equipo.getMotivoDisolucion()).isEqualTo("Nos retiramos de la liga");
        assertThat(resp.estado()).isEqualTo("DISUELTO");
        verify(equipoRepository).save(equipo);
    }

    @Test
    void disolver_acepta_motivo_vacio_y_lo_guarda_como_null() {
        Equipo equipo = equipoActivo();
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(false);
        when(partidaRepository.existsPartidaPendientePorEquipo(10L)).thenReturn(false);
        when(equipoRepository.save(equipo)).thenReturn(equipo);

        service.disolver(10L, new DisolverEquipoRequest(true, "   "), "cap@x.com");

        assertThat(equipo.getMotivoDisolucion()).isNull();
    }

    @Test
    void disolver_falla_si_el_solicitante_no_es_capitan() {
        when(usuarioRepository.findByCorreo("jug@x.com")).thenReturn(Optional.of(jugador));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L))
                .thenReturn(Optional.of(miembro(jugador, "TITULAR", "ACTIVO")));

        assertThatThrownBy(() -> service.disolver(10L, new DisolverEquipoRequest(true, null), "jug@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capitán");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_falla_si_el_solicitante_no_pertenece_al_equipo() {
        when(usuarioRepository.findByCorreo("jug@x.com")).thenReturn(Optional.of(jugador));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disolver(10L, new DisolverEquipoRequest(true, null), "jug@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permisos");
    }

    @Test
    void disolver_falla_si_el_equipo_ya_esta_disuelto() {
        Equipo disuelto = equipoActivo();
        disuelto.setEstado("DISUELTO");
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(disuelto));

        assertThatThrownBy(() -> service.disolver(10L, new DisolverEquipoRequest(true, null), "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está disuelto");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_bloquea_si_hay_inscripciones_activas_en_torneos() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.disolver(10L, new DisolverEquipoRequest(true, null), "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inscripciones activas");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_bloquea_si_hay_partidas_pendientes() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(false);
        when(partidaRepository.existsPartidaPendientePorEquipo(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.disolver(10L, new DisolverEquipoRequest(true, null), "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("partidas pendientes");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_lanza_404_si_el_equipo_no_existe() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disolver(10L, new DisolverEquipoRequest(true, null), "cap@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
