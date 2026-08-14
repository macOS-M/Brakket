package com.coffeecommits.brakket.twitch.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coffeecommits.brakket.twitch.event.MuestraChatCapturadaEvent;
import com.coffeecommits.brakket.twitch.model.MensajeChat;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MensajeChatRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Muestreo periodico del chat de Twitch (RF-38).
 *
 * <p>Mismo patron que MuestreoAudienciaService: un tick por intervalo, el
 * canal sale de la transmision abierta y no se inventan datos cuando la
 * fuente no responde.</p>
 *
 * <p>Se persisten los agregados por ventana y tambien el contenido de cada
 * mensaje con su hora (RF-38). El <b>autor no se guarda</b>: el nick solo vive
 * en memoria para contar usuarios distintos.</p>
 *
 * <p><b>El conteo y la clasificacion van a ritmos distintos.</b> Contar es
 * gratis y conviene fino: de ahi sale el detalle por minuto de las metricas.
 * Clasificar el sentimiento (RF-39) cuesta una llamada al proveedor, y hacerla
 * por ventana gastaba 1440 llamadas diarias para pintar puntos que el termometro
 * despues promedia en tramos de 5 a 60 minutos. Los textos se acumulan en
 * memoria a lo largo de un bloque y se clasifican de una sola vez.</p>
 */
@Service
@Slf4j
public class MuestreoChatService {

    /** Twitch corta los mensajes en 500; el recorte es defensivo. */
    private static final int MAX_LARGO_TEXTO = 500;

    private final ChatTwitchListener listener;
    private final TransmisionTwitchRepository transmisionRepository;
    private final MetricaChatRepository metricaChatRepository;
    private final MensajeChatRepository mensajeChatRepository;
    private final ApplicationEventPublisher eventos;

    /** Se necesita para convertir el conteo de la ventana a tasa por minuto. */
    private final long intervaloMs;

    /** Cada cuanto se clasifica el sentimiento del chat acumulado. */
    private final long bloqueMinutos;

    /** Tope de mensajes que se le pasan al analizador por bloque. */
    private final int maxMensajesBloque;

    public MuestreoChatService(ChatTwitchListener listener,
                               TransmisionTwitchRepository transmisionRepository,
                               MetricaChatRepository metricaChatRepository,
                               MensajeChatRepository mensajeChatRepository,
                               ApplicationEventPublisher eventos,
                               @Value("${brakket.streams.muestreo-intervalo-ms:60000}") long intervaloMs,
                               @Value("${brakket.streams.sentimiento-bloque-minutos:5}") long bloqueMinutos,
                               @Value("${brakket.streams.sentimiento-max-mensajes-bloque:180}") int maxMensajesBloque) {
        this.listener = listener;
        this.transmisionRepository = transmisionRepository;
        this.metricaChatRepository = metricaChatRepository;
        this.mensajeChatRepository = mensajeChatRepository;
        this.eventos = eventos;
        this.intervaloMs = intervaloMs;
        this.bloqueMinutos = bloqueMinutos;
        this.maxMensajesBloque = maxMensajesBloque;
    }

    /** Canal al que ya estamos conectados; null si todavia no hay conexion. */
    private volatile String canalConectado;

    /**
     * Textos del bloque en curso. Viven aca hasta que se clasifican y despues se
     * sueltan: no se persisten ni salen a ningun lado que no sea el analizador.
     */
    private final List<String> textosDelBloque = new ArrayList<>();

    /** Inicio del bloque en curso; null cuando no hay nada acumulado. */
    private LocalDateTime bloqueDesde;

    /** Muestra a la que se colgara el analisis del bloque. */
    private Long ultimaMuestraId;

    /**
     * El bloque lo tocan dos hilos: el del planificador y el de la peticion que
     * dispara {@link #clasificarAhora()}. Sin candado, un cierre manual en medio
     * de un tick podria mandar textos a medio acumular o perderlos.
     */
    private final Object candadoBloque = new Object();

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
                // Antes de soltar todo se clasifica lo que quedo a medio bloque:
                // una transmision mas corta que el bloque se quedaria sin una
                // sola lectura de sentimiento.
                synchronized (candadoBloque) {
                    cerrarBloque();
                }
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

        persistirMensajes(transmision, ventana);

        synchronized (candadoBloque) {
            acumular(muestra.getId(), ventana.textos());
            if (bloqueCumplido()) {
                cerrarBloque();
            }
        }

        log.info("Chat de #{}: {} mensajes en la ventana -> {}/min, {} autores",
                canal, ventana.mensajes(), porMinuto, ventana.autoresDistintos());
    }

    /**
     * Guarda el contenido de la ventana con la hora de cada mensaje (RF-38).
     *
     * <p>Va en la misma transacción que la métrica: si falla el guardado, la
     * muestra tampoco queda, y es preferible perder la ventana entera antes que
     * dejar un agregado que dice "300 mensajes" sin los mensajes detrás.</p>
     */
    private void persistirMensajes(TransmisionTwitch transmision, ChatTwitchListener.Ventana ventana) {
        if (ventana.capturados().isEmpty()) {
            return;
        }
        mensajeChatRepository.saveAll(ventana.capturados().stream()
                .map(capturado -> MensajeChat.builder()
                        .transmisionTwitch(transmision)
                        .texto(recortar(capturado.texto()))
                        .fechaHora(capturado.instante())
                        .build())
                .toList());
    }

    private String recortar(String texto) {
        return texto.length() > MAX_LARGO_TEXTO ? texto.substring(0, MAX_LARGO_TEXTO) : texto;
    }

    // --- bloque de sentimiento ----------------------------------------------

    /**
     * Cierra el bloque en curso y lo manda a clasificar sin esperar el intervalo.
     *
     * <p>Existe para no quedar atado a la cadencia cuando hace falta una lectura
     * ya —una demostracion, un momento puntual de la transmision—. Devuelve
     * cuantos mensajes se mandaron a analizar, o cero si todavia no hay chat
     * acumulado.</p>
     *
     * <p>Es {@code @Transactional} a proposito, aunque no escriba: el listener de
     * RF-39 espera el evento en AFTER_COMMIT, asi que publicarlo fuera de una
     * transaccion lo perderia en silencio.</p>
     */
    @Transactional
    public int clasificarAhora() {
        synchronized (candadoBloque) {
            return cerrarBloque();
        }
    }

    private void acumular(Long muestraId, List<String> textos) {
        ultimaMuestraId = muestraId;
        if (textos.isEmpty()) {
            return;
        }
        if (bloqueDesde == null) {
            bloqueDesde = LocalDateTime.now();
        }
        textosDelBloque.addAll(textos);
    }

    private boolean bloqueCumplido() {
        return bloqueDesde != null
                && !LocalDateTime.now().isBefore(bloqueDesde.plusMinutes(bloqueMinutos));
    }

    /**
     * Manda a clasificar lo acumulado y arranca un bloque nuevo.
     *
     * <p>Va por evento para no acoplar el muestreo al modulo de analytics, y se
     * consume despues del commit: un fallo del analisis no puede tirar la
     * muestra. El analisis se cuelga de la ultima muestra del bloque, asi que la
     * serie de RF-40 queda con un punto por bloque en vez de uno por minuto.</p>
     */
    private int cerrarBloque() {
        if (textosDelBloque.isEmpty() || ultimaMuestraId == null) {
            reiniciarBloque();
            return 0;
        }
        List<String> muestra = repartidos(textosDelBloque, maxMensajesBloque);
        log.info("Sentimiento: se clasifica un bloque de {} mensajes ({} enviados al analizador).",
                textosDelBloque.size(), muestra.size());
        eventos.publishEvent(new MuestraChatCapturadaEvent(ultimaMuestraId, muestra));
        reiniciarBloque();
        return muestra.size();
    }

    private void reiniciarBloque() {
        textosDelBloque.clear();
        bloqueDesde = null;
    }

    /**
     * Recorta a {@code tope} tomando mensajes repartidos por todo el bloque.
     *
     * <p>No se toman los ultimos, que es lo barato: en un bloque de quince
     * minutos eso clasificaria el ultimo medio minuto de chat y lo presentaria
     * como el clima del cuarto de hora.</p>
     */
    private List<String> repartidos(List<String> todos, int tope) {
        if (todos.size() <= tope) {
            return List.copyOf(todos);
        }
        List<String> muestra = new ArrayList<>(tope);
        double paso = todos.size() / (double) tope;
        for (int i = 0; i < tope; i++) {
            muestra.add(todos.get((int) (i * paso)));
        }
        return muestra;
    }

    /** Mismo criterio que MuestreoAudienciaService para resolver el canal. */
    private String login(TransmisionTwitch t) {
        String login = t.getLoginCanal() != null ? t.getLoginCanal()
                : (t.getCanal() == null ? null : t.getCanal().getLoginCanal());
        return login == null || login.isBlank() ? null : login.trim().toLowerCase(Locale.ROOT);
    }
}