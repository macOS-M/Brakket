package com.coffeecommits.brakket.twitch.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Lector anonimo del chat de Twitch (RF-38).
 *
 * <p>El chat de Twitch es IRC sobre WebSocket: lineas de texto planas
 * separadas por CRLF. Conectarse como "justinfan" permite leer cualquier
 * canal publico sin token ni scopes, asi que esta captura no toca el flujo
 * OAuth de RF-34.</p>
 *
 * <p>Solo acumula en memoria; no persiste nada. Quien vuelca la ventana a
 * metrica_chat es el muestreador programado.</p>
 */

@Component
@Slf4j
public  class ChatTwitchListener {

    private static final URI IRC = URI.create("wss://irc-ws.chat.twitch.tv:443");

    public record Ventana(int mensajes, int autoresDistintos, List <String> textos) {
    }

    private final Object candado = new Object();
    private final List<String> textos = new ArrayList<>();
    private final Set<String> autores = new HashSet<>();

    private volatile WebSocket webSocket;

    /**
     * false apenas la conexion se cae. El muestreador lo consulta para
     * reconectar: sin esto seguiria guardando ventanas de cero mensajes como
     * si el chat estuviera mudo, en vez de volver a conectarse.
     */
    private volatile boolean conectado;

    public void conectar(String canal) {
        String login = canal.trim().toLowerCase(Locale.ROOT).replace("#", "");
        // Cerrar la conexion anterior antes de abrir otra: si no, quedan dos
        // WebSockets vivos alimentando el MISMO acumulador y la ventana mezcla
        // mensajes de dos canales sin que nada falle ni se note.
        desconectar();
        webSocket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(IRC, new OyenteIrc())
                .join();
        int sufijo = ThreadLocalRandom.current().nextInt(10_000, 99_999);
        webSocket.sendText("NICK justinfan" + sufijo + "\r\n", true);
        webSocket.sendText("JOIN #" + login + "\r\n", true);
        conectado = true;
        log.info("Conectado al chat de #{}", login);
    }

    /** false si nunca se conecto o si la conexion se cayo. */
    public boolean estaConectado() {
        return conectado;
    }

    /**
     * Cierra la conexion vigente (si la hay) y descarta lo acumulado: los
     * mensajes del canal viejo no pertenecen a la ventana del nuevo.
     */
    public void desconectar() {
        WebSocket actual = webSocket;
        webSocket = null;
        conectado = false;
        if (actual != null) {
            try {
                actual.sendClose(WebSocket.NORMAL_CLOSURE, "cambio de canal");
            } catch (Exception ex) {
                log.debug("No se pudo cerrar limpiamente el WebSocket anterior: {}", ex.getMessage());
            } finally {
                actual.abort();
            }
        }
        synchronized (candado) {
            textos.clear();
            autores.clear();
        }
    }

    /** Devuelve lo acumulado y deja los contadores en cero. */
    public Ventana tomarYReiniciar() {
        synchronized (candado) {
            Ventana ventana = new Ventana(textos.size(), autores.size(), List.copyOf(textos));
            textos.clear();
            autores.clear();
            return ventana;
        }
    }

    private void procesar(WebSocket ws, String linea) {
        // El servidor hace PING cada pocos minutos; sin PONG nos desconecta.
        if (linea.startsWith("PING")) {
            ws.sendText("PONG :tmi.twitch.tv\r\n", true);
            return;
        }
        // :autor!autor@autor.tmi.twitch.tv PRIVMSG #canal :el mensaje
        int privmsg = linea.indexOf(" PRIVMSG ");
        int finAutor = linea.indexOf('!');
        if (!linea.startsWith(":") || privmsg < 0 || finAutor < 1 || finAutor > privmsg) {
            return; // ruido del protocolo (JOIN, 001, NAMES, etc.)
        }
        int inicioTexto = linea.indexOf(" :", privmsg);
        if (inicioTexto < 0) {
            return;
        }
        String autor = linea.substring(1, finAutor);
        String texto = linea.substring(inicioTexto + 2);
        synchronized (candado) {
            textos.add(texto);
            autores.add(autor);
        }
    }

    private class OyenteIrc implements WebSocket.Listener {

        /**
         * Buffer propio de esta conexion: onText puede entregar media linea y
         * hay que acumular hasta que last sea true. Es por instancia y no del
         * componente para que un fragmento a medias de una conexion vieja no
         * se mezcle con la primera linea de la nueva.
         */
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String completo = buffer.toString();
                buffer.setLength(0);
                for (String linea : completo.split("\r\n")) {
                    if (!linea.isBlank()) {
                        procesar(ws, linea);
                    }
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int codigo, String motivo) {
            marcarCaida(ws, "cerrado por el servidor (" + codigo + " " + motivo + ")");
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            marcarCaida(ws, error.getMessage());
        }
    }

    /**
     * Marca la caida solo si el evento viene del socket vigente: al cambiar de
     * canal cerramos el anterior a proposito, y su onClose no debe pisar el
     * estado de la conexion nueva.
     */
    private void marcarCaida(WebSocket ws, String motivo) {
        if (ws != webSocket) {
            return;
        }
        conectado = false;
        log.warn("Conexion con el chat de Twitch caida: {}. Se reintenta en el proximo tick.", motivo);
    }
}