package com.coffeecommits.brakket.notification.controller;

import com.coffeecommits.brakket.notification.dto.NotificacionResponse;
import com.coffeecommits.brakket.notification.model.EstadoEntrega;
import com.coffeecommits.brakket.notification.model.TipoNotificacion;
import com.coffeecommits.brakket.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final String CORREO = "jugador@brakket.local";

    @Mock
    private NotificationService notificationService;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private NotificationController controller;

    @Test
    void listar_usa_la_identidad_autenticada() {
        NotificacionResponse response = notificacion();
        when(authentication.getName()).thenReturn(CORREO);
        when(notificationService.listar(CORREO)).thenReturn(List.of(response));

        assertThat(controller.listar(authentication)).containsExactly(response);
    }

    @Test
    void contarNoLeidas_devuelve_un_json_con_la_clave_count() {
        when(authentication.getName()).thenReturn(CORREO);
        when(notificationService.contarNoLeidas(CORREO)).thenReturn(4L);

        Map<String, Long> resultado = controller.contarNoLeidas(authentication);

        assertThat(resultado).containsEntry("count", 4L);
    }

    @Test
    void marcarLeida_delega_id_y_usuario() {
        NotificacionResponse response = notificacion();
        when(authentication.getName()).thenReturn(CORREO);
        when(notificationService.marcarLeida(CORREO, 8L)).thenReturn(response);

        assertThat(controller.marcarLeida(8L, authentication)).isEqualTo(response);
    }

    @Test
    void marcarTodasLeidas_responde_204() {
        when(authentication.getName()).thenReturn(CORREO);

        ResponseEntity<Void> respuesta = controller.marcarTodasLeidas(authentication);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(notificationService).marcarTodasLeidas(CORREO);
    }

    @Test
    void eliminar_responde_204_y_delega_la_eliminacion_logica() {
        when(authentication.getName()).thenReturn(CORREO);

        ResponseEntity<Void> respuesta = controller.eliminar(8L, authentication);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(204);
        verify(notificationService).eliminarDeBandeja(CORREO, 8L);
    }

    private NotificacionResponse notificacion() {
        return new NotificacionResponse(8L, TipoNotificacion.DISPUTA, "Se abrió una disputa",
                "Arbitraje", "disputa", 20L, false, LocalDateTime.now(),
                EstadoEntrega.DISPONIBLE);
    }
}
