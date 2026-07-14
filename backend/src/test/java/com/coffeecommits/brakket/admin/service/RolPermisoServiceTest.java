package com.coffeecommits.brakket.admin.service;

import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Permiso;
import com.coffeecommits.brakket.auth.model.Rol;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import com.coffeecommits.brakket.auth.repository.RolRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.JerarquiaInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolPermisoServiceTest {

    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private LogAuditoriaRepository logAuditoriaRepository;
    @InjectMocks
    private RolPermisoService rolPermisoService;

    private static final String CORREO_ADMIN = "admin@brakket.gg";

    private Permiso gestionarRoles() {
        return Permiso.builder().id(1L).codigo("GESTIONAR_ROLES").build();
    }

    private Rol rolAdmin() {
        return Rol.builder().id(1L).nombreRol("ADMIN").nivel(1)
                .permisos(Set.of(gestionarRoles())).build();
    }

    private Rol rolJugador() {
        return Rol.builder().id(5L).nombreRol("JUGADOR").nivel(5)
                .permisos(Set.of()).build();
    }

    private Usuario admin() {
        return Usuario.builder().id(1L).nombre("Admin").correo(CORREO_ADMIN).build();
    }

    private UsuarioRol asignacion(Usuario usuario, Rol rol) {
        return UsuarioRol.builder().usuario(usuario).rol(rol).build();
    }

    @Test
    void asignarRol_guarda_y_registra_auditoria() {
        Usuario objetivo = Usuario.builder().id(2L).nombre("Meta").correo("meta@brakket.gg").build();
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rolJugador()));
        when(usuarioRepository.findByCorreo(CORREO_ADMIN)).thenReturn(Optional.of(admin()));
        when(usuarioRolRepository.findByUsuarioId(1L)).thenReturn(List.of(asignacion(admin(), rolAdmin())));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(objetivo));
        when(usuarioRolRepository.existsByUsuarioIdAndRolId(2L, 5L)).thenReturn(false);
        when(usuarioRolRepository.findByUsuarioId(2L)).thenReturn(List.of(asignacion(objetivo, rolJugador())));

        rolPermisoService.asignarRol(2L, 5L, CORREO_ADMIN);

        verify(usuarioRolRepository).save(any(UsuarioRol.class));
        verify(logAuditoriaRepository).save(any());
    }

    @Test
    void asignarRol_rechaza_rol_de_mayor_jerarquia_que_el_actor() {
        Usuario jugador = Usuario.builder().id(3L).nombre("Juan").correo("juan@brakket.gg").build();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin()));
        when(usuarioRepository.findByCorreo("juan@brakket.gg")).thenReturn(Optional.of(jugador));
        when(usuarioRolRepository.findByUsuarioId(3L)).thenReturn(List.of(asignacion(jugador, rolJugador())));

        assertThatThrownBy(() -> rolPermisoService.asignarRol(2L, 1L, "juan@brakket.gg"))
                .isInstanceOf(JerarquiaInvalidaException.class);
        verify(usuarioRolRepository, never()).save(any(UsuarioRol.class));
    }

    @Test
    void asignarRol_rechaza_cuenta_destino_bloqueada() {
        Usuario bloqueado = Usuario.builder().id(2L).nombre("Beto").correo("beto@brakket.gg")
                .bloqueado(true).build();
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rolJugador()));
        when(usuarioRepository.findByCorreo(CORREO_ADMIN)).thenReturn(Optional.of(admin()));
        when(usuarioRolRepository.findByUsuarioId(1L)).thenReturn(List.of(asignacion(admin(), rolAdmin())));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(bloqueado));

        assertThatThrownBy(() -> rolPermisoService.asignarRol(2L, 5L, CORREO_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloqueada");
        verify(usuarioRolRepository, never()).save(any(UsuarioRol.class));
    }

    @Test
    void asignarRol_rechaza_asignacion_duplicada() {
        Usuario objetivo = Usuario.builder().id(2L).nombre("Meta").correo("meta@brakket.gg").build();
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rolJugador()));
        when(usuarioRepository.findByCorreo(CORREO_ADMIN)).thenReturn(Optional.of(admin()));
        when(usuarioRolRepository.findByUsuarioId(1L)).thenReturn(List.of(asignacion(admin(), rolAdmin())));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(objetivo));
        when(usuarioRolRepository.existsByUsuarioIdAndRolId(2L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> rolPermisoService.asignarRol(2L, 5L, CORREO_ADMIN))
                .isInstanceOf(BusinessException.class);
        verify(usuarioRolRepository, never()).save(any(UsuarioRol.class));
    }

    @Test
    void revocarRol_bloquea_al_ultimo_gestor_de_roles() {
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin()));
        when(usuarioRepository.findByCorreo(CORREO_ADMIN)).thenReturn(Optional.of(admin()));
        when(usuarioRolRepository.findByUsuarioId(1L)).thenReturn(List.of(asignacion(admin(), rolAdmin())));
        when(usuarioRolRepository.findByUsuarioIdAndRolId(1L, 1L))
                .thenReturn(Optional.of(asignacion(admin(), rolAdmin())));
        when(usuarioRolRepository.contarAsignacionesConPermiso("GESTIONAR_ROLES")).thenReturn(1L);

        assertThatThrownBy(() -> rolPermisoService.revocarRol(1L, 1L, CORREO_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("último");
        verify(usuarioRolRepository, never()).delete(any());
    }

    @Test
    void revocarRol_permite_revocar_admin_cuando_queda_otro_gestor() {
        Usuario otroAdmin = Usuario.builder().id(2L).nombre("Meta").correo("meta@brakket.gg").build();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin()));
        when(usuarioRepository.findByCorreo(CORREO_ADMIN)).thenReturn(Optional.of(admin()));
        when(usuarioRolRepository.findByUsuarioId(1L)).thenReturn(List.of(asignacion(admin(), rolAdmin())));
        when(usuarioRolRepository.findByUsuarioIdAndRolId(2L, 1L))
                .thenReturn(Optional.of(asignacion(otroAdmin, rolAdmin())));
        when(usuarioRolRepository.contarAsignacionesConPermiso("GESTIONAR_ROLES")).thenReturn(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroAdmin));
        when(usuarioRolRepository.findByUsuarioId(2L)).thenReturn(List.of());

        rolPermisoService.revocarRol(2L, 1L, CORREO_ADMIN);

        verify(usuarioRolRepository).delete(any(UsuarioRol.class));
        verify(logAuditoriaRepository).save(any());
    }
}
