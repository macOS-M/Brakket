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

        List<String> mensajes = request.mensajes().stream()
                .filter(m -> m != null && !m.isBlank())
                .toList();
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
                .mensajesPorMinuto(mensajes.size())
                .usuariosActivos(usuariosActivos)
                .build());

        AnalizadorSentimiento.Resultado resultado = analizador.analizar(mensajes);

        AnalisisSentimiento analisis = analisisRepository.save(AnalisisSentimiento.builder()
                .metricaChat(metrica)
                .fechaHora(ahora)
                .clasificacion(resultado.clasificacion().name())
                .puntaje(resultado.puntaje())
                .build());

        return SentimientoResponse.from(analisis);
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
