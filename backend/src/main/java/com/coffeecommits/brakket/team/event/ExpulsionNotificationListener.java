package com.coffeecommits.brakket.team.event;

import com.coffeecommits.brakket.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RF-10: notifica al integrante expulsado, pero solo una vez que la baja se
 * confirmó en la base (AFTER_COMMIT). Así se cumple el criterio de la ERS de
 * que un fallo al notificar no deshaga la expulsión: cuando este listener
 * corre, la transacción de la expulsión ya commiteó y es intocable.
 *
 * REQUIRES_NEW porque tras el commit ya no hay transacción activa: el envío
 * necesita la suya propia. Si falla, se registra y se descarta.
 */
@Component
public class ExpulsionNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ExpulsionNotificationListener.class);

    private final NotificationService notificationService;

    public ExpulsionNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alExpulsarIntegrante(IntegranteExpulsadoEvent evento) {
        try {
            notificationService.notificar(
                    evento.destinatario(),
                    "EXPULSION_EQUIPO",
                    "Fuiste removido del equipo '%s'. Causa: %s".formatted(
                            evento.equipoNombre(), evento.causa()),
                    "MiembroEquipo",
                    evento.miembroId());
        } catch (RuntimeException ex) {
            log.warn("No se pudo notificar la expulsion del miembro {}: {}",
                    evento.miembroId(), ex.getMessage());
        }
    }
}
