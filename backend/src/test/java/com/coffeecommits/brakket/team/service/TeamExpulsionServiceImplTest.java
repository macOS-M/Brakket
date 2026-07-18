package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.notification.service.NotificationService;
import com.coffeecommits.brakket.team.dto.ExpulsarIntegranteRequest;
import com.coffeecommits.brakket.team.dto.MiembroEquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamExpulsionServiceImplTest {

    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private TeamExpulsionServiceImpl service;

    private final Equipo equipo = Equipo.builder().id(10L).nombre("Coffee&Commits").estado("ACTIVO").build();
    private final Usuario capitan = Usuario.builder().id(1L).correo("cap@x.com").nombre("Capi").build();
    private final Usuario jugador = Usuario.builder().id(2L).correo("jug@x.com").nombre("Juga").build();

    private final ExpulsarIntegranteRequest request = new ExpulsarIntegranteRequest("Inasistencia reiterada");

    private MiembroEquipo miembro(Usuario usuario, String rol, String estado) {
        return MiembroEquipo.builder()
                .id(usuario.getId() + 100)
                .equipo(equipo)
                .usuario(usuario)
                .estado(estado)
                .rol(rol)
                .build();
    }

    private void prepararCapitan() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L))
                .thenReturn(Optional.of(miembro(capitan, "CAPITAN", "ACTIVO")));
    }

    @Test
    void expulsa_a_un_integrante_activo_y_registra_la_baja() {
        prepararCapitan();
        MiembroEquipo objetivo = miembro(jugador, "TITULAR", "ACTIVO");
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L)).thenReturn(Optional.of(objetivo));

        MiembroEquipoResponse resp = service.expulsar(10L, 2L, request, "cap@x.com");

        assertThat(resp.estado()).isEqualTo("EXPULSADO");
        assertThat(objetivo.getFechaBaja()).isNotNull();
        assertThat(objetivo.getCausaBaja()).isEqualTo("Inasistencia reiterada");
        assertThat(objetivo.getResponsableBaja()).isEqualTo(capitan);
        verify(miembroEquipoRepository).save(objetivo);
        verify(notificationService).notificar(any(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void la_baja_queda_registrada_aunque_falle_la_notificacion() {
        prepararCapitan();
        MiembroEquipo objetivo = miembro(jugador, "TITULAR", "ACTIVO");
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L)).thenReturn(Optional.of(objetivo));
        doThrow(new RuntimeException("smtp caido"))
                .when(notificationService).notificar(any(), anyString(), anyString(), anyString(), anyLong());

        MiembroEquipoResponse resp = service.expulsar(10L, 2L, request, "cap@x.com");

        assertThat(resp.estado()).isEqualTo("EXPULSADO");
        verify(miembroEquipoRepository).save(objetivo);
    }

    @Test
    void falla_si_el_solicitante_no_es_capitan_activo() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(usuarioRepository.findByCorreo("jug@x.com")).thenReturn(Optional.of(jugador));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L))
                .thenReturn(Optional.of(miembro(jugador, "TITULAR", "ACTIVO")));

        assertThatThrownBy(() -> service.expulsar(10L, 1L, request, "jug@x.com"))
                .isInstanceOf(ForbiddenException.class);

        verify(miembroEquipoRepository, never()).save(any());
    }

    @Test
    void falla_si_el_capitan_esta_inactivo() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L))
                .thenReturn(Optional.of(miembro(capitan, "CAPITAN", "INACTIVO")));

        assertThatThrownBy(() -> service.expulsar(10L, 2L, request, "cap@x.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void falla_si_el_integrante_no_pertenece_al_equipo() {
        prepararCapitan();
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.expulsar(10L, 99L, request, "cap@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void falla_si_el_integrante_no_esta_activo() {
        prepararCapitan();
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L))
                .thenReturn(Optional.of(miembro(jugador, "TITULAR", "EXPULSADO")));

        assertThatThrownBy(() -> service.expulsar(10L, 2L, request, "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("activo");
    }

    @Test
    void no_permite_expulsar_al_unico_capitan() {
        prepararCapitan();
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L))
                .thenReturn(Optional.of(miembro(capitan, "CAPITAN", "ACTIVO")));
        when(miembroEquipoRepository.countByEquipoIdAndRolAndEstado(10L, "CAPITAN", "ACTIVO"))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.expulsar(10L, 1L, request, "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capitan");

        verify(miembroEquipoRepository, never()).save(any());
    }

    @Test
    void falla_si_el_equipo_esta_disuelto() {
        Equipo disuelto = Equipo.builder().id(10L).nombre("Coffee&Commits").estado("DISUELTO").build();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(disuelto));

        assertThatThrownBy(() -> service.expulsar(10L, 2L, request, "cap@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disuelto");
    }

    @Test
    void falla_si_el_equipo_no_existe() {
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.expulsar(99L, 2L, request, "cap@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
