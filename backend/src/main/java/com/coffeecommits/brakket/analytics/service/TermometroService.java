package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.dto.DistribucionSentimiento;
import com.coffeecommits.brakket.analytics.dto.IntervaloSentimiento;
import com.coffeecommits.brakket.analytics.dto.TermometroResponse;
import com.coffeecommits.brakket.analytics.dto.TransmisionAnalizadaResponse;
import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import com.coffeecommits.brakket.analytics.model.EstadoTermometro;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.config.AnalyticsProperties;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Termómetro de sentimiento de una transmisión (RF-40, EPIC-10).
 *
 * <p>Lee lo que RF-39 dejó persistido y lo convierte en lo que la vista
 * necesita: indicador, distribución, evolución por intervalos y un resumen en
 * texto. No calcula sentimiento ni toca mensajes de chat; de eso se ocupa
 * {@link SentimientoService}.</p>
 *
 * <p>El resumen se arma acá y no en el cliente porque la ERS lo lista como dato
 * de salida: así queda uno solo, testeable, y no una frase distinta por cada
 * vista que muestre el termómetro.</p>
 */
@Service
public class TermometroService {

    /**
     * Cotas para el período abierto. Son fechas reales y no {@code LocalDateTime.MIN/MAX}
     * porque esos años no entran en un {@code timestamp} de Postgres; el rango
     * que cubren excede de sobra la vida de cualquier transmisión.
     */
    private static final LocalDateTime SIN_LIMITE_INFERIOR = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime SIN_LIMITE_SUPERIOR = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final AnalisisSentimientoRepository analisisRepository;
    private final TransmisionTwitchRepository transmisionRepository;
    private final AnalyticsProperties properties;

    public TermometroService(AnalisisSentimientoRepository analisisRepository,
                             TransmisionTwitchRepository transmisionRepository,
                             AnalyticsProperties properties) {
        this.analisisRepository = analisisRepository;
        this.transmisionRepository = transmisionRepository;
        this.properties = properties;
    }

    /** Transmisiones con análisis disponible, para el selector del termómetro. */
    @Transactional(readOnly = true)
    public List<TransmisionAnalizadaResponse> transmisionesAnalizadas() {
        return analisisRepository.findTransmisionesAnalizadas();
    }

    @Transactional(readOnly = true)
    public TermometroResponse consultar(Long transmisionId, LocalDateTime desde,
                                        LocalDateTime hasta, Integer intervaloMinutos) {
        if (!transmisionRepository.existsById(transmisionId)) {
            throw new ResourceNotFoundException("Transmisión", transmisionId);
        }
        int intervalo = validarPeriodo(desde, hasta, intervaloMinutos);

        List<AnalisisSentimiento> serie = analisisRepository.findSerieByTransmisionEntre(
                transmisionId,
                desde != null ? desde : SIN_LIMITE_INFERIOR,
                hasta != null ? hasta : SIN_LIMITE_SUPERIOR);

        if (serie.isEmpty()) {
            return sinIndicador(transmisionId, EstadoTermometro.PENDIENTE, desde, hasta, intervalo,
                    DistribucionSentimiento.de(List.of()), List.of(), 0);
        }

        DistribucionSentimiento distribucion =
                DistribucionSentimiento.de(serie.stream().map(AnalisisSentimiento::getClasificacion).toList());
        List<IntervaloSentimiento> intervalos = agrupar(serie, intervalo);

        if (serie.size() < properties.getMinimoMuestras()) {
            // Hay señal, pero no la suficiente: se devuelven distribución e
            // intervalos para que la vista pueda mostrar el detalle, y el
            // indicador general en null para que no lo pinte.
            return sinIndicador(transmisionId, EstadoTermometro.INSUFICIENTE, desde, hasta, intervalo,
                    distribucion, intervalos, serie.size());
        }

        BigDecimal puntaje = promedio(serie.stream().map(AnalisisSentimiento::getPuntaje).toList());
        ClasificacionSentimiento clasificacion = ClasificacionSentimiento.desdePuntaje(puntaje);

        return new TermometroResponse(transmisionId, EstadoTermometro.DISPONIBLE,
                resumenDisponible(clasificacion, puntaje, distribucion, serie),
                desde, hasta, intervalo,
                puntaje, clasificacion.name(),
                serie.size(), properties.getMinimoMuestras(),
                distribucion, intervalos);
    }

    private TermometroResponse sinIndicador(Long transmisionId, EstadoTermometro estado,
                                            LocalDateTime desde, LocalDateTime hasta, int intervalo,
                                            DistribucionSentimiento distribucion,
                                            List<IntervaloSentimiento> intervalos, long muestras) {
        return new TermometroResponse(transmisionId, estado,
                estado == EstadoTermometro.PENDIENTE ? RESUMEN_PENDIENTE : resumenInsuficiente(muestras),
                desde, hasta, intervalo,
                null, null,
                muestras, properties.getMinimoMuestras(),
                distribucion, intervalos);
    }

    /**
     * Valida el período y resuelve el ancho del intervalo. Se rechaza en vez de
     * corregir en silencio: un rango invertido casi siempre es un filtro mal
     * armado, y devolver una serie vacía lo haría parecer "no hay datos".
     */
    private int validarPeriodo(LocalDateTime desde, LocalDateTime hasta, Integer intervaloMinutos) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("El inicio del período no puede ser posterior al final.");
        }
        int intervalo = intervaloMinutos != null
                ? intervaloMinutos
                : properties.getIntervaloPorDefectoMinutos();
        if (intervalo <= 0) {
            throw new BusinessException("El intervalo debe ser de al menos un minuto.");
        }
        if (desde != null && hasta != null) {
            long tramos = Duration.between(desde, hasta).toMinutes() / intervalo + 1;
            if (tramos > properties.getMaxIntervalos()) {
                throw new BusinessException(
                        "El período consultado da más de " + properties.getMaxIntervalos()
                                + " intervalos. Ampliá el intervalo o acortá el período.");
            }
        }
        return intervalo;
    }

    /**
     * Agrupa la serie en tramos de ancho fijo, anclados en la primera muestra.
     * Los tramos sin muestras no se emiten: un hueco no es un cero, que en esta
     * escala significa "chat neutro".
     */
    private List<IntervaloSentimiento> agrupar(List<AnalisisSentimiento> serie, int intervaloMinutos) {
        List<IntervaloSentimiento> intervalos = new ArrayList<>();
        Duration ancho = Duration.ofMinutes(intervaloMinutos);

        LocalDateTime inicio = serie.get(0).getFechaHora();
        List<BigDecimal> puntajes = new ArrayList<>();

        for (AnalisisSentimiento analisis : serie) {
            while (!analisis.getFechaHora().isBefore(inicio.plus(ancho))) {
                if (!puntajes.isEmpty()) {
                    intervalos.add(tramo(inicio, ancho, puntajes));
                    puntajes = new ArrayList<>();
                }
                inicio = inicio.plus(ancho);
            }
            puntajes.add(analisis.getPuntaje());
        }
        if (!puntajes.isEmpty()) {
            intervalos.add(tramo(inicio, ancho, puntajes));
        }
        return intervalos;
    }

    private IntervaloSentimiento tramo(LocalDateTime inicio, Duration ancho, List<BigDecimal> puntajes) {
        BigDecimal promedio = promedio(puntajes);
        return new IntervaloSentimiento(inicio, inicio.plus(ancho), puntajes.size(), promedio,
                ClasificacionSentimiento.desdePuntaje(promedio).name());
    }

    private static BigDecimal promedio(List<BigDecimal> puntajes) {
        BigDecimal suma = puntajes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(puntajes.size()), 2, RoundingMode.HALF_UP);
    }

    // ---------- Resumen textual ----------

    private static final String RESUMEN_PENDIENTE =
            "Todavía no hay análisis de sentimiento para esta transmisión en el período consultado. "
                    + "Mientras la transmisión esté en vivo el chat se captura cada minuto, pero el "
                    + "sentimiento se clasifica por bloques de varios minutos: la primera lectura "
                    + "tarda un poco en aparecer.";

    private String resumenInsuficiente(long muestras) {
        return "Hay " + muestras + (muestras == 1 ? " muestra" : " muestras")
                + " en el período consultado y se necesitan al menos " + properties.getMinimoMuestras()
                + " para un indicador confiable. El termómetro se muestra cuando haya más chat analizado.";
    }

    private String resumenDisponible(ClasificacionSentimiento clasificacion, BigDecimal puntaje,
                                     DistribucionSentimiento distribucion,
                                     List<AnalisisSentimiento> serie) {
        String ambiente = switch (clasificacion) {
            case POSITIVO -> "El ambiente del chat es mayormente positivo";
            case NEGATIVO -> "El ambiente del chat es mayormente negativo";
            case NEUTRO -> "El ambiente del chat es neutro";
        };
        return ambiente + " (puntaje " + puntaje.stripTrailingZeros().toPlainString()
                + " en una escala de -100 a 100). De " + serie.size() + " muestras analizadas, "
                + distribucion.positivo() + " positivas, " + distribucion.neutro() + " neutras y "
                + distribucion.negativo() + " negativas. " + tendencia(serie)
                + " Es una lectura orientativa del clima de la audiencia, no una evaluación de usuarios.";
    }

    /**
     * Compara la primera mitad del período con la segunda. Con menos de cuatro
     * muestras no se afirma nada: dos puntos no describen una tendencia.
     */
    private String tendencia(List<AnalisisSentimiento> serie) {
        if (serie.size() < 4) {
            return "Aún no hay muestras suficientes para hablar de una tendencia.";
        }
        int mitad = serie.size() / 2;
        BigDecimal antes = promedio(serie.subList(0, mitad).stream()
                .map(AnalisisSentimiento::getPuntaje).toList());
        BigDecimal despues = promedio(serie.subList(mitad, serie.size()).stream()
                .map(AnalisisSentimiento::getPuntaje).toList());
        BigDecimal cambio = despues.subtract(antes);

        // 10 puntos de la escala: por debajo de eso el movimiento entra dentro
        // del ruido normal entre ventanas y no vale la pena nombrarlo.
        if (cambio.abs().compareTo(BigDecimal.TEN) < 0) {
            return "Se mantuvo estable a lo largo del período.";
        }
        return cambio.signum() > 0
                ? "Viene mejorando respecto del inicio del período."
                : "Viene empeorando respecto del inicio del período.";
    }
}
