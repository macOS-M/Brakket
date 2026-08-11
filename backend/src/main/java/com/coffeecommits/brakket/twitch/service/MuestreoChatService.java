package com.coffeecommits.brakket.twitch.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coffeecommits.brakket.twitch.event.MuestraChatCapturadaEvent;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Muestreo periodico del chat de Twitch (RF-38).
 *
 * <p>Mismo patron que MuestreoAudienciaService: un tick por intervalo, el
 * canal sale de la transmision abierta y no se inventan datos cuando la
 * fuente no responde. Los mensajes se cuentan en memoria y se descartan; solo
 * se persisten los agregados (RNF de datos personales).</p>
 */
@Service
@Slf4j
public class MuestreoChatService {

    private final ChatTwitchListener listener;
    private final TransmisionTwitchRepository transmisionRepository;
    private final MetricaChatRepository metricaChatRepository;
    private final ApplicationEventPublisher eventos;

    /** Se necesita para convertir el conteo de la ventana a tasa por minuto. */
    private final long intervaloMs;

    public MuestreoChatService(ChatTwitchListener listener,
                               TransmisionTwitchRepository transmisionRepository,
                               MetricaChatRepository metricaChatRepository,
                               ApplicationEventPublisher eventos,
                               @Value("${brakket.streams.muestreo-intervalo-ms:60000}") long intervaloMs) {
        this.listener = listener;
        this.transmisionRepository = transmisionRepository;
        this.metricaChatRepository = metricaChatRepository;
        this.eventos = eventos;
        this.intervaloMs = intervaloMs;
    }

    /** Canal al que ya estamos conectados; null si todavia no hay conexion. */
    private volatile String canalConectado;

    @Scheduled(fixedDelayString = "${brakket.streams.muestreo-intervalo-ms:60000}",
            initialDelayString = "${brakket.streams.muestreo-espera-inicial-ms:15000}")
    @Transactional
    public void muestrear() {
        // Hoy se captura una sola transmision a la vez. La consulta esta
        // ordenada por id para que la eleccion sea estable entre ticks: sin
        // orden, con dos transmisiones abiertas el listener podria alternar
        // entre canales y tirar la ventana en cada cambio.
        TransmisionTwitch transmision = transmisionRepository.findAbiertasParaMuestreo().stream()
                .filter(t -> t.getPlataforma() == PlataformaTransmision.TWITCH)
                .filter(t -> login(t) != null)
                .findFirst()
                .orElse(null);
        if (transmision == null) {
            // Sin transmision abierta no hay chat que capturar. Se suelta la
            // conexion: al cerrar la ultima transmision el socket quedaba vivo
            // escuchando un canal que ya nadie mira.
            if (canalConectado != null) {
                listener.desconectar();
                canalConectado = null;
                log.info("Sin transmisiones abiertas: se cierra la conexion con el chat.");
            }
            return;
        }
        String canal = login(transmision);

        // Reconecta si cambio el canal o si la conexion se cayo. Sin lo segundo
        // el muestreo seguiria guardando ventanas de cero mensajes como si el
        // chat estuviera mudo, que es peor que no guardar nada.
        if (!canal.equals(canalConectado) || !listener.estaConectado()) {
            try {
                listener.conectar(canal);
                canalConectado = canal;
                log.info("Chat de #{}: conectado. La primera ventana se lee en el proximo tick.", canal);
            } catch (Exception ex) {
                canalConectado = null;
                log.warn("No se pudo conectar al chat de #{}: {}", canal, ex.getMessage());
            }
            // Este tick no escribe muestra: el ERS prohibe inventar valores y
            // la ventana que acaba de empezar no cubre el intervalo completo.
            return;
        }

        ChatTwitchListener.Ventana ventana = listener.tomarYReiniciar();
        // La ventana puede no durar un minuto (el intervalo es configurable)
        int porMinuto = (int) Math.round(ventana.mensajes() * 60000.0 / intervaloMs);

        MetricaChat muestra = metricaChatRepository.save(MetricaChat.builder()
                .transmisionTwitch(transmision)
                .fechaHora(LocalDateTime.now())
                .mensajesPorMinuto(porMinuto)
                .usuariosActivos(ventana.autoresDistintos())
                .build());

        // RF-39 clasifica el sentimiento de esta misma ventana. Va por evento
        // para no acoplar el muestreo al modulo de analytics, y se consume
        // despues del commit: un fallo del analisis no puede tirar la muestra.
        // Los textos viajan en memoria hasta el analizador y no se persisten.
        if (!ventana.textos().isEmpty()) {
            eventos.publishEvent(new MuestraChatCapturadaEvent(muestra.getId(), ventana.textos()));
        }

        log.info("Chat de #{}: {} mensajes en la ventana -> {}/min, {} autores",
                canal, ventana.mensajes(), porMinuto, ventana.autoresDistintos());
    }

    /** Mismo criterio que MuestreoAudienciaService para resolver el canal. */
    private String login(TransmisionTwitch t) {
        String login = t.getLoginCanal() != null ? t.getLoginCanal()
                : (t.getCanal() == null ? null : t.getCanal().getLoginCanal());
        return login == null || login.isBlank() ? null : login.trim().toLowerCase(Locale.ROOT);
    }
}