package com.coffeecommits.brakket.team.event;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpulsionNotificationListenerTest {

    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private ExpulsionNotificationListener listener;

    private final IntegranteExpulsadoEvent evento = new IntegranteExpulsadoEvent(
            100L, Usuario.builder().id(2L).nombre("Juga").build(), "Coffee&Commits", "Inasistencia");

    @Test
    void notifica_al_expulsado() {
        listener.alExpulsarIntegrante(evento);

        verify(notificationService).notificar(
                any(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void un_fallo_al_notificar_no_se_propaga() {
        // La baja ya commiteó cuando este listener corre; un fallo al notificar
        // debe registrarse y descartarse, nunca romper el flujo.
        doThrow(new RuntimeException("smtp caido"))
                .when(notificationService).notificar(any(), anyString(), anyString(), anyString(), anyLong());

        assertThatCode(() -> listener.alExpulsarIntegrante(evento)).doesNotThrowAnyException();
    }
}
