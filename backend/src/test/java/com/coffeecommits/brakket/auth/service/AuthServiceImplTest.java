package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.model.Rol;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import com.coffeecommits.brakket.auth.repository.RolRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void upsert_crea_usuario_nuevo_y_le_asigna_rol_base_jugador() {
        when(usuarioRepository.findByGoogleId("g-1")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(usuarioRolRepository.findByUsuarioId(10L)).thenReturn(List.of());
        when(rolRepository.findByNombreRol("JUGADOR"))
                .thenReturn(Optional.of(Rol.builder().id(5L).nombreRol("JUGADOR").build()));

        Usuario result = authService.upsertGoogleUser("g-1", "ana@brakket.gg", "Ana", "foto.png");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCorreo()).isEqualTo("ana@brakket.gg");
        verify(usuarioRolRepository).save(any(UsuarioRol.class));
    }

    @Test
    void upsert_usuario_existente_con_rol_no_duplica_el_rol() {
        Usuario existente = Usuario.builder().id(7L).googleId("g-1").correo("viejo@x.com").build();
        when(usuarioRepository.findByGoogleId("g-1")).thenReturn(Optional.of(existente));
        when(usuarioRolRepository.findByUsuarioId(7L)).thenReturn(List.of(
                UsuarioRol.builder().usuario(existente)
                        .rol(Rol.builder().nombreRol("JUGADOR").build()).build()));

        authService.upsertGoogleUser("g-1", "nuevo@x.com", "Ana", "foto.png");

        assertThat(existente.getCorreo()).isEqualTo("nuevo@x.com");
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRolRepository, never()).save(any(UsuarioRol.class));
    }

    @Test
    void getCurrentUser_mapea_usuario_y_roles() {
        Usuario u = Usuario.builder().id(7L).nombre("Ana").correo("ana@brakket.gg").fotoUrl("foto.png").build();
        when(usuarioRepository.findByCorreo("ana@brakket.gg")).thenReturn(Optional.of(u));
        when(usuarioRolRepository.findByUsuarioId(7L)).thenReturn(List.of(
                UsuarioRol.builder().usuario(u).rol(Rol.builder().nombreRol("JUGADOR").build()).build()));

        UsuarioResponse resp = authService.getCurrentUser("ana@brakket.gg");

        assertThat(resp.authenticated()).isTrue();
        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.foto()).isEqualTo("foto.png");
        assertThat(resp.roles()).containsExactly("JUGADOR");
    }

    @Test
    void getCurrentUser_lanza_404_si_no_existe() {
        when(usuarioRepository.findByCorreo("nadie@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("nadie@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
