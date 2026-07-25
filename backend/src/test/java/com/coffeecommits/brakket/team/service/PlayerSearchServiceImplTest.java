package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.JugadorDisponibleResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerSearchServiceImplTest {

    private static final String CORREO = "capitan@brakket.gg";
    private static final long EQUIPO_ID = 10L;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @InjectMocks
    private PlayerSearchServiceImpl service;

    private Usuario usuario(long id, String nombre) {
        return Usuario.builder().id(id).nombre(nombre).correo(nombre + "@x.gg").build();
    }

    private Equipo equipo(long id, String nombre) {
        return Equipo.builder().id(id).nombre(nombre).estado("ACTIVO").build();
    }

    private void equipoExiste() {
        when(equipoRepository.findById(EQUIPO_ID)).thenReturn(Optional.of(equipo(EQUIPO_ID, "Mi Equipo")));
    }

    /** Deja al solicitante como capitan activo del equipo. */
    private void capitanValido() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(1L, "Capi")));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(EQUIPO_ID, 1L))
                .thenReturn(Optional.of(MiembroEquipo.builder().rol("CAPITAN").build()));
    }

    @Test
    void falla_si_el_equipo_no_existe() {
        when(equipoRepository.findById(EQUIPO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(EQUIPO_ID, "", null, false, CORREO, 0, 12))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void falla_si_el_solicitante_no_es_miembro() {
        equipoExiste();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(1L, "Capi")));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(EQUIPO_ID, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(EQUIPO_ID, "", null, false, CORREO, 0, 12))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No tiene permisos");
    }

    @Test
    void falla_si_el_miembro_no_es_capitan() {
        equipoExiste();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(1L, "Capi")));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(EQUIPO_ID, 1L))
                .thenReturn(Optional.of(MiembroEquipo.builder().rol("TITULAR").build()));

        assertThatThrownBy(() -> service.buscar(EQUIPO_ID, "", null, false, CORREO, 0, 12))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Solo el capitan");
    }

    @Test
    void candidato_sin_equipo_es_invitacion_directa() {
        equipoExiste();
        capitanValido();
        Usuario libre = usuario(50L, "Libre");
        when(usuarioRepository.buscarDisponibles(any(), any(), eq(EQUIPO_ID), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(libre), PageRequest.of(0, 12), 1));
        when(miembroEquipoRepository.findActivosByUsuarioIds(List.of(50L))).thenReturn(List.of());

        PageResponse<JugadorDisponibleResponse> res =
                service.buscar(EQUIPO_ID, "", null, false, CORREO, 0, 12);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).requiereTransferencia()).isFalse();
        assertThat(res.items().get(0).equipoActualNombre()).isNull();
    }

    @Test
    void candidato_activo_en_otro_equipo_requiere_transferencia() {
        equipoExiste();
        capitanValido();
        Usuario fichado = usuario(60L, "Fichado");
        when(usuarioRepository.buscarDisponibles(any(), any(), eq(EQUIPO_ID), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fichado), PageRequest.of(0, 12), 1));
        MiembroEquipo enOtro = MiembroEquipo.builder()
                .usuario(fichado).equipo(equipo(99L, "Otro Equipo")).estado("ACTIVO").build();
        when(miembroEquipoRepository.findActivosByUsuarioIds(List.of(60L))).thenReturn(List.of(enOtro));

        PageResponse<JugadorDisponibleResponse> res =
                service.buscar(EQUIPO_ID, "", null, false, CORREO, 0, 12);

        assertThat(res.items().get(0).requiereTransferencia()).isTrue();
        assertThat(res.items().get(0).equipoActualNombre()).isEqualTo("Otro Equipo");
    }

    @Test
    void pagina_vacia_no_consulta_los_badges() {
        equipoExiste();
        capitanValido();
        when(usuarioRepository.buscarDisponibles(any(), any(), eq(EQUIPO_ID), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        PageResponse<JugadorDisponibleResponse> res =
                service.buscar(EQUIPO_ID, "", null, false, CORREO, 0, 12);

        assertThat(res.items()).isEmpty();
        assertThat(res.totalElements()).isZero();
        // El guard ids.isEmpty() evita la query de badges cuando no hay candidatos.
        verify(miembroEquipoRepository, never()).findActivosByUsuarioIds(any());
    }
}
