package com.coffeecommits.brakket.notification.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.notification.dto.NotificacionResponse;
import com.coffeecommits.brakket.notification.model.EstadoEntrega;
import com.coffeecommits.brakket.notification.model.Notificacion;
import com.coffeecommits.brakket.notification.model.TipoNotificacion;
import com.coffeecommits.brakket.notification.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final String CORREO = "jugador@brakket.local";

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private NotificationServiceImpl service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(7L).correo(CORREO).nombre("Jugador").build();
    }

    @Test
    void notificar_crea_una_notificacion_valida() {
        when(notificacionRepository.existsByUsuarioIdAndTipoAndEntidadAndEntidadIdAndMensajeAndEliminadaBandejaFalse(
                7L, "DISPUTA", "disputa", 25L, "Debes aportar evidencia"))
                .thenReturn(false);

        service.notificar(usuario, TipoNotificacion.DISPUTA, " Debes aportar evidencia ",
                "Sistema de arbitraje", "disputa", 25L);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion creada = captor.getValue();
        assertThat(creada.getUsuario()).isEqualTo(usuario);
        assertThat(creada.getTipo()).isEqualTo("DISPUTA");
        assertThat(creada.getMensaje()).isEqualTo("Debes aportar evidencia");
        assertThat(creada.getOrigen()).isEqualTo("Sistema de arbitraje");
        assertThat(creada.getLeida()).isFalse();
        assertThat(creada.getEstadoEntrega()).isEqualTo(EstadoEntrega.DISPONIBLE);
        assertThat(creada.getEliminadaBandeja()).isFalse();
    }

    @Test
    void notificar_no_duplica_el_mismo_evento() {
        when(notificacionRepository.existsByUsuarioIdAndTipoAndEntidadAndEntidadIdAndMensajeAndEliminadaBandejaFalse(
                7L, "DISPUTA", "disputa", 25L, "Debes aportar evidencia"))
                .thenReturn(true);

        service.notificar(usuario, TipoNotificacion.DISPUTA, "Debes aportar evidencia",
                "Arbitraje", "disputa", 25L);

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void notificar_rechaza_datos_obligatorios_incompletos() {
        assertThatThrownBy(() -> service.notificar(
                usuario, TipoNotificacion.DISPUTA, " ", "Arbitraje", "disputa", 25L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destinatario, tipo, evento y motivo");
    }

    @Test
    void listar_devuelve_solo_la_bandeja_del_usuario_ordenada_por_repositorio() {
        Notificacion notificacion = notificacion(9L, false);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.findByUsuarioIdAndEliminadaBandejaFalseOrderByFechaDesc(7L))
                .thenReturn(List.of(notificacion));

        List<NotificacionResponse> resultado = service.listar(CORREO, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().id()).isEqualTo(9L);
        assertThat(resultado.getFirst().tipo()).isEqualTo(TipoNotificacion.DISPUTA);
    }

    @Test
    void listar_aplica_un_limite_acotado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.findByUsuarioIdAndEliminadaBandejaFalseOrderByFechaDesc(
                org.mockito.ArgumentMatchers.eq(7L), any(Pageable.class)))
                .thenReturn(List.of(notificacion(9L, false)));

        assertThat(service.listar(CORREO, 5)).hasSize(1);

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacionRepository).findByUsuarioIdAndEliminadaBandejaFalseOrderByFechaDesc(
                org.mockito.ArgumentMatchers.eq(7L), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void listar_no_falla_por_un_tipo_legacy_desconocido() {
        Notificacion legacy = notificacion(10L, false);
        legacy.setTipo("TIPO_RETIRADO");
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.findByUsuarioIdAndEliminadaBandejaFalseOrderByFechaDesc(7L))
                .thenReturn(List.of(legacy));

        assertThat(service.listar(CORREO, null).getFirst().tipo())
                .isEqualTo(TipoNotificacion.ADMINISTRATIVA);
    }

    @Test
    void contarNoLeidas_excluye_leidas_y_eliminadas_mediante_el_repositorio() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalseAndEliminadaBandejaFalse(7L))
                .thenReturn(3L);

        assertThat(service.contarNoLeidas(CORREO)).isEqualTo(3L);
    }

    @Test
    void marcarLeida_actualiza_una_notificacion_del_usuario() {
        Notificacion notificacion = notificacion(9L, false);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.findByIdAndUsuarioId(9L, 7L))
                .thenReturn(Optional.of(notificacion));

        NotificacionResponse resultado = service.marcarLeida(CORREO, 9L);

        assertThat(notificacion.getLeida()).isTrue();
        assertThat(resultado.leida()).isTrue();
    }

    @Test
    void marcarLeida_no_permite_acceder_a_una_notificacion_ajena() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.findByIdAndUsuarioId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida(CORREO, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Notificación no encontrada");
    }

    @Test
    void marcarTodasLeidas_actualiza_todas_las_pendientes() {
        Notificacion primera = notificacion(1L, false);
        Notificacion segunda = notificacion(2L, false);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.findByUsuarioIdAndLeidaFalse(7L))
                .thenReturn(List.of(primera, segunda));

        service.marcarTodasLeidas(CORREO);

        assertThat(primera.getLeida()).isTrue();
        assertThat(segunda.getLeida()).isTrue();
    }

    @Test
    void eliminarDeBandeja_realiza_eliminacion_logica() {
        Notificacion notificacion = notificacion(9L, true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.findByIdAndUsuarioId(9L, 7L))
                .thenReturn(Optional.of(notificacion));

        service.eliminarDeBandeja(CORREO, 9L);

        assertThat(notificacion.getEliminadaBandeja()).isTrue();
        verify(notificacionRepository, never()).delete(any());
    }

    private Notificacion notificacion(Long id, boolean leida) {
        return Notificacion.builder()
                .id(id)
                .usuario(usuario)
                .tipo("DISPUTA")
                .mensaje("Se abrió una disputa")
                .origen("Arbitraje")
                .entidad("disputa")
                .entidadId(25L)
                .leida(leida)
                .fecha(LocalDateTime.now())
                .estadoEntrega(EstadoEntrega.DISPONIBLE)
                .eliminadaBandeja(false)
                .build();
    }
}
