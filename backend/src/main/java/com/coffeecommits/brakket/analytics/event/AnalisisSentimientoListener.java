package com.coffeecommits.brakket.analytics.event;

import com.coffeecommits.brakket.analytics.service.SentimientoService;
import com.coffeecommits.brakket.twitch.event.MuestraChatCapturadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RF-39: clasifica el chat que acaba de capturar el muestreo de RF-38, con lo
 * que el análisis deja de depender de que un admin pegue los mensajes a mano.
 *
 * <p>Mismo patrón que {@code ExpulsionNotificationListener}: AFTER_COMMIT y
 * REQUIRES_NEW para que el análisis no pueda tumbar la muestra. Interesa sobre
 * todo cuando el motor sea un proveedor de IA remoto (RNF-23): un timeout suyo
 * debe perder el análisis de esa ventana, nunca la métrica de chat ya
 * confirmada ni el tick del muestreo.</p>
 */
@Component
public class AnalisisSentimientoListener {

    private static final Logger log = LoggerFactory.getLogger(AnalisisSentimientoListener.class);

    private final SentimientoService sentimientoService;

    public AnalisisSentimientoListener(SentimientoService sentimientoService) {
        this.sentimientoService = sentimientoService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCapturarMuestraDeChat(MuestraChatCapturadaEvent evento) {
        try {
            sentimientoService.analizarMuestra(evento.metricaChatId(), evento.mensajes());
        } catch (RuntimeException ex) {
            log.warn("No se pudo analizar el sentimiento de la muestra {}: {}",
                    evento.metricaChatId(), ex.getMessage());
        }
    }
}
