package com.coffeecommits.brakket.twitch.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class MuestreoChatService {

    private final ChatTwitchListener listener;
    private final TransmisionTwitchRepository transmisionRepository;
    private final MetricaChatRepository metricaChatRepository;

    /** Se necesita para convertir el conteo de la ventana a tasa por minuto. */
    @Value("${brakket.streams.muestreo-intervalo-ms:60000}")
    private long intervaloMs;

    /** Canal al que ya estamos conectados; null si todavia no hay conexion. */
    private volatile String canalConectado;

    @Scheduled(fixedDelayString = "${brakket.streams.muestreo-intervalo-ms:60000}",
            initialDelayString = "${brakket.streams.muestreo-espera-inicial-ms:15000}")
    @Transactional
    public void muestrear() {
        TransmisionTwitch transmision = transmisionRepository.findAbiertasParaMuestreo().stream()
                .filter(t -> t.getPlataforma() == PlataformaTransmision.TWITCH)
                .filter(t -> login(t) != null)
                .findFirst()
                .orElse(null);
        if (transmision == null) {
            return; // sin transmision abierta no hay chat que capturar
        }
        String canal = login(transmision);

        if (!canal.equals(canalConectado)) {
            try {
                listener.conectar(canal);
                canalConectado = canal;
                log.info("Chat de #{}: conectado. La primera ventana se lee en el proximo tick.", canal);
            } catch (Exception ex) {
                log.warn("No se pudo conectar al chat de #{}: {}", canal, ex.getMessage());
            }
            return;
        }

        ChatTwitchListener.Ventana ventana = listener.tomarYReiniciar();
        // La ventana puede no durar un minuto (el intervalo es configurable)
        int porMinuto = (int) Math.round(ventana.mensajes() * 60000.0 / intervaloMs);

        metricaChatRepository.save(MetricaChat.builder()
                .transmisionTwitch(transmision)
                .fechaHora(LocalDateTime.now())
                .mensajesPorMinuto(porMinuto)
                .usuariosActivos(ventana.autoresDistintos())
                .build());

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