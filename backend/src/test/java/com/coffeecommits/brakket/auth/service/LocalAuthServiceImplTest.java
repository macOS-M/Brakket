package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.dto.LoginLocalRequest;
import com.coffeecommits.brakket.auth.dto.RegistroLocalRequest;
import com.coffeecommits.brakket.auth.model.Rol;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import com.coffeecommits.brakket.auth.repository.RolRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.config.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private LocalAuthServiceImpl service;

    @Test
    void registrar_normaliza_correo_hashea_password_y_asigna_solo_jugador() {
        when(usuarioRepository.findByCorreo("ana@brakket.gg")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(rolRepository.findByNombreRol("JUGADOR"))
                .thenReturn(Optional.of(Rol.builder().id(5L).nombreRol("JUGADOR").build()));
        when(jwtService.generateToken(anyString(), anyMap())).thenReturn("jwt-emitido");

        String token = service.registrar(
                new RegistroLocalRequest("Ana", "  Ana@Brakket.GG ", "contrasena-segura"));

        assertThat(token).isEqualTo("jwt-emitido");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getCorreo()).isEqualTo("ana@brakket.gg");
        assertThat(guardado.getGoogleId()).isNull();
        assertThat(guardado.getPasswordHash()).isNotEqualTo("contrasena-segura");
        assertThat(new BCryptPasswordEncoder()
                .matches("contrasena-segura", guardado.getPasswordHash())).isTrue();
        // Solo el rol base: registrarse localmente jamás otorga ADMIN.
        ArgumentCaptor<UsuarioRol> rolCaptor = ArgumentCaptor.forClass(UsuarioRol.class);
        verify(usuarioRolRepository).save(rolCaptor.capture());
        assertThat(rolCaptor.getValue().getRol().getNombreRol()).isEqualTo("JUGADOR");
    }

    @Test
    void registrar_rechaza_correo_existente() {
        when(usuarioRepository.findByCorreo("ana@brakket.gg"))
                .thenReturn(Optional.of(Usuario.builder().id(1L).correo("ana@brakket.gg").build()));

        assertThatThrownBy(() -> service.registrar(
                new RegistroLocalRequest("Ana", "ana@brakket.gg", "contrasena-segura")))
                .isInstanceOf(BusinessException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_valida_credenciales_sin_revelar_cual_fallo() {
        String hash = new BCryptPasswordEncoder().encode("correcta-123");
        Usuario usuario = Usuario.builder().id(1L).correo("ana@brakket.gg")
                .nombre("Ana").passwordHash(hash).build();
        when(usuarioRepository.findByCorreo("ana@brakket.gg")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(anyString(), anyMap())).thenReturn("jwt-emitido");

        assertThat(service.login(new LoginLocalRequest("ana@brakket.gg", "correcta-123")))
                .isEqualTo("jwt-emitido");

        assertThatThrownBy(() -> service.login(new LoginLocalRequest("ana@brakket.gg", "incorrecta")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Correo o contraseña");
    }

    @Test
    void login_avisa_si_la_cuenta_es_de_google() {
        Usuario deGoogle = Usuario.builder().id(1L).correo("g@x.com").googleId("g-1").build();
        when(usuarioRepository.findByCorreo("g@x.com")).thenReturn(Optional.of(deGoogle));

        assertThatThrownBy(() -> service.login(new LoginLocalRequest("g@x.com", "lo-que-sea")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Google");
    }
}
