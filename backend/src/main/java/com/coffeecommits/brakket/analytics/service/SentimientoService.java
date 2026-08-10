package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.dto.AnalizarChatRequest;
import com.coffeecommits.brakket.analytics.dto.PuntoSentimiento;
import com.coffeecommits.brakket.analytics.dto.SentimientoResponse;
import com.coffeecommits.brakket.analytics.dto.SerieSentimientoResponse;
import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Análisis de sentimiento del chat de una transmisión (RF-39, EPIC-10).
 *
 * <p>Cada análisis persiste una muestra {@link MetricaChat} (volumen de chat de
 * la ventana) y su {@link AnalisisSentimiento} (clasificación + puntaje), de modo
 * que RF-40 pueda dibujar el termómetro y su evolución en el tiempo.</p>
 */
@Service
public class SentimientoService {

    private final AnalisisSentimientoRepository analisisRepository;
    private final MetricaChatRepository metricaChatRepository;
    private final TransmisionTwitchRepository transmisionRepository;
    private final AnalizadorSentimiento analizador;

    public SentimientoService(AnalisisSentimientoRepository analisisRepository,
                              MetricaChatRepository metricaChatRepository,
                              TransmisionTwitchRepository transmisionRepository,
                              AnalizadorSentimiento analizador) {
        this.analisisRepository = analisisRepository;
        this.metricaChatRepository = metricaChatRepository;
        this.transmisionRepository = transmisionRepository;
        this.analizador = analizador;
    }

    @Transactional
    public SentimientoResponse analizar(Long transmisionId, AnalizarChatRequest request) {
        TransmisionTwitch transmision = buscarTransmision(transmisionId);

        List<String> mensajes = conContenido(request.mensajes());
        if (mensajes.isEmpty()) {
            throw new BusinessException("No hay mensajes de chat con contenido para analizar.");
        }

        int usuariosActivos = request.usuariosActivos() != null && request.usuariosActivos() > 0
                ? request.usuariosActivos()
                : mensajes.size();

        LocalDateTime ahora = LocalDateTime.now();
        MetricaChat metrica = metricaChatRepository.save(MetricaChat.builder()
                .transmisionTwitch(transmision)
                .fechaHora(ahora)
                .mensajesPorMinuto(porMinuto(mensajes.size(), request.ventanaSegundos()))
                .usuariosActivos(usuariosActivos)
                .build());

        AnalisisSentimiento analisis = guardarAnalisis(metrica, mensajes, ahora);
        return SentimientoResponse.from(analisis, mensajes.size());
    }

    /**
     * Analiza la ventana que acaba de capturar el muestreo de RF-38 y le cuelga
     * el resultado a su muestra. Es el camino automático del RF: el manual
     * ({@link #analizar}) queda como herramienta de administración y demo.
     *
     * <p>Los textos llegan por parámetro y no se persisten: de la ventana solo
     * queda el agregado, igual que en RF-38.</p>
     */
    @Transactional
    public void analizarMuestra(Long metricaChatId, List<String> mensajes) {
        List<String> utiles = conContenido(mensajes);
        if (utiles.isEmpty()) {
            return; // ventana muda: no hay señal que clasificar
        }
        // La muestra podría haber desaparecido entre el commit del muestreo y
        // este análisis (borrado de la transmisión en cascada); no es un error.
        metricaChatRepository.findById(metricaChatId)
                .ifPresent(metrica -> guardarAnalisis(metrica, utiles, metrica.getFechaHora()));
    }

    private AnalisisSentimiento guardarAnalisis(MetricaChat metrica, List<String> mensajes,
                                                LocalDateTime fechaHora) {
        AnalizadorSentimiento.Resultado resultado = analizador.analizar(mensajes);
        return analisisRepository.save(AnalisisSentimiento.builder()
                .metricaChat(metrica)
                .fechaHora(fechaHora)
                .clasificacion(resultado.clasificacion().name())
                .puntaje(resultado.puntaje())
                .build());
    }

    private static List<String> conContenido(List<String> mensajes) {
        return mensajes.stream().filter(m -> m != null && !m.isBlank()).toList();
    }

    /**
     * Convierte el conteo del lote a la tasa por minuto que guarda
     * {@code metrica_chat}, para que las muestras manuales sean comparables con
     * las que escribe el muestreo automático de RF-38 en esa misma columna.
     */
    private static int porMinuto(int mensajes, Integer ventanaSegundos) {
        int ventana = ventanaSegundos != null && ventanaSegundos > 0
                ? ventanaSegundos
                : AnalizarChatRequest.VENTANA_POR_DEFECTO_SEGUNDOS;
        return (int) Math.round(mensajes * 60.0 / ventana);
    }

    @Transactional(readOnly = true)
    public SerieSentimientoResponse serie(Long transmisionId) {
        buscarTransmision(transmisionId); // 404 si la transmisión no existe
        List<AnalisisSentimiento> serie = analisisRepository.findSerieByTransmision(transmisionId);

        List<PuntoSentimiento> puntos = serie.stream().map(PuntoSentimiento::from).toList();
        SentimientoResponse ultimo = serie.isEmpty()
                ? null
                : SentimientoResponse.from(serie.get(serie.size() - 1));
        BigDecimal promedio = promedioPuntaje(serie);

        return new SerieSentimientoResponse(transmisionId, ultimo, promedio, serie.size(), puntos);
    }

    private BigDecimal promedioPuntaje(List<AnalisisSentimiento> serie) {
        if (serie.isEmpty()) {
            return null;
        }
        BigDecimal suma = serie.stream()
                .map(AnalisisSentimiento::getPuntaje)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(serie.size()), 2, RoundingMode.HALF_UP);
    }

    private TransmisionTwitch buscarTransmision(Long transmisionId) {
        return transmisionRepository.findById(transmisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transmisión", transmisionId));
    }
}
