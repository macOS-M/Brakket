package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.team.dto.AsignarRolRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.EquipoRolHistorial;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRolHistorialRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamRoleServiceImplTest {

    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EquipoRolHistorialRepository historialRepository;
    @InjectMocks
    private TeamRoleServiceImpl teamRoleService;

    private final Equipo equipo = Equipo.builder().id(10L).nombre("Coffee&Commits").build();
    private final Usuario capitan = Usuario.builder().id(1L).correo("cap@x.com").nombre("Capi").build();
    private final Usuario jugador = Usuario.builder().id(2L).correo("jug@x.com").nombre("Juga").build();

    private MiembroEquipo miembro(Usuario usuario, String rol) {
        return MiembroEquipo.builder()
                .id(usuario.getId() + 100)
                .equipo(equipo)
                .usuario(usuario)
                .estado("ACTIVO")
                .rol(rol)
                .build();
    }

    @Test
    void cambiarRol_actualiza_el_rol_y_registra_historial() {
        MiembroEquipo miembroCapitan = miembro(capitan, "CAPITAN");
        MiembroEquipo miembroJugador = miembro(jugador, "TITULAR");
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(miembroCapitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L)).thenReturn(Optional.of(miembroJugador));
        when(miembroEquipoRepository.save(miembroJugador)).thenReturn(miembroJugador);

        MiembroEquipoResponse resp = teamRoleService.cambiarRol(
                10L, 2L, new AsignarRolRequest("suplente"), "cap@x.com");

        assertThat(resp.rol()).isEqualTo("SUPLENTE");
        ArgumentCaptor<EquipoRolHistorial> captor = ArgumentCaptor.forClass(EquipoRolHistorial.class);
        verify(historialRepository).save(captor.capture());
        assertThat(captor.getValue().getRolAnterior()).isEqualTo("TITULAR");
        assertThat(captor.getValue().getRolNuevo()).isEqualTo("SUPLENTE");
        assertThat(captor.getValue().getResponsable()).isEqualTo(capitan);
    }

    @Test
    void cambiarRol_falla_si_el_solicitante_no_es_capitan() {
        when(usuarioRepository.findByCorreo("jug@x.com")).thenReturn(Optional.of(jugador));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L))
                .thenReturn(Optional.of(miembro(jugador, "TITULAR")));

        assertThatThrownBy(() -> teamRoleService.cambiarRol(
                10L, 1L, new AsignarRolRequest("SUPLENTE"), "jug@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capitan");
        verify(miembroEquipoRepository, never()).save(any(MiembroEquipo.class));
    }

    @Test
    void cambiarRol_rechaza_un_rol_fuera_de_la_lista_valida() {
        assertThatThrownBy(() -> teamRoleService.cambiarRol(
                10L, 2L, new AsignarRolRequest("DIRECTOR"), "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Rol invalido");
        verify(miembroEquipoRepository, never()).save(any(MiembroEquipo.class));
        verify(historialRepository, never()).save(any(EquipoRolHistorial.class));
    }

    @Test
    void cambiarRol_no_deja_al_equipo_sin_capitan_activo() {
        MiembroEquipo miembroCapitan = miembro(capitan, "CAPITAN");
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(miembroCapitan));
        when(miembroEquipoRepository.countByEquipoIdAndRolAndEstado(10L, "CAPITAN", "ACTIVO")).thenReturn(1L);

        assertThatThrownBy(() -> teamRoleService.cambiarRol(
                10L, 1L, new AsignarRolRequest("TITULAR"), "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capitan activo");
        verify(miembroEquipoRepository, never()).save(any(MiembroEquipo.class));
    }

    @Test
    void cambiarRol_al_mismo_rol_no_registra_historial() {
        MiembroEquipo miembroCapitan = miembro(capitan, "CAPITAN");
        MiembroEquipo miembroJugador = miembro(jugador, "TITULAR");
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(miembroCapitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L)).thenReturn(Optional.of(miembroJugador));

        MiembroEquipoResponse resp = teamRoleService.cambiarRol(
                10L, 2L, new AsignarRolRequest("TITULAR"), "cap@x.com");

        assertThat(resp.rol()).isEqualTo("TITULAR");
        verify(miembroEquipoRepository, never()).save(any(MiembroEquipo.class));
        verify(historialRepository, never()).save(any(EquipoRolHistorial.class));
    }

    @Test
    void cambiarRol_falla_si_el_integrante_no_esta_activo() {
        MiembroEquipo miembroCapitan = miembro(capitan, "CAPITAN");
        MiembroEquipo inactivo = MiembroEquipo.builder()
                .id(102L).equipo(equipo).usuario(jugador).estado("INACTIVO").rol("TITULAR").build();
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(miembroCapitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> teamRoleService.cambiarRol(
                10L, 2L, new AsignarRolRequest("SUPLENTE"), "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("activo");
    }

    @Test
    void listarMiembros_mapea_la_plantilla_a_dto() {
        when(miembroEquipoRepository.findByEquipoId(10L)).thenReturn(List.of(
                miembro(capitan, "CAPITAN"), miembro(jugador, "TITULAR")));

        List<MiembroEquipoResponse> resp = teamRoleService.listarMiembros(10L);

        assertThat(resp).hasSize(2);
        assertThat(resp).extracting(MiembroEquipoResponse::rol)
                .containsExactly("CAPITAN", "TITULAR");
        assertThat(resp).extracting(MiembroEquipoResponse::nombreUsuario)
                .containsExactly("Capi", "Juga");
    }
}
