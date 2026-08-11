package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Analizador de sentimiento basado en un léxico (RF-39).
 *
 * <p>Cuenta términos positivos y negativos (español, inglés y jerga de chat de
 * esports) sobre el lote de mensajes y deriva un puntaje en [-100, 100] a partir
 * de la proporción entre ambos. Es <b>determinista</b> (mismo lote ⇒ mismo
 * resultado), no depende de servicios externos y por eso es demostrable offline.
 * Un proveedor de IA real puede sustituirlo por la interfaz
 * {@link AnalizadorSentimiento} sin cambiar nada aguas arriba (RNF-23).</p>
 */
@Component
public class AnalizadorLexico implements AnalizadorSentimiento {

    /** Extrae palabras respetando acentos/ñ (letras y dígitos Unicode). */
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");

    // "ez" y "wtf" quedaron fuera a propósito: en chat de esports "ez" suele ser
    // burla al rival y "wtf" es tan seguido sorpresa positiva como queja, así que
    // como términos de signo fijo metían más ruido del que aportaban.
    private static final Set<String> POSITIVAS = Set.of(
            "gg", "ggwp", "pog", "poggers", "pogchamp", "hype", "hypee", "increible",
            "increíble", "excelente", "genial", "bueno", "buena", "buenardo", "win",
            "clutch", "limpio", "crack", "vamos", "vamooos", "amo", "love",
            "nice", "great", "goat", "insane", "epic", "epico", "épico", "wow",
            "bravo", "hermoso", "brutal", "fenomeno", "fenómeno", "top", "letsgo",
            "gogo", "king", "queen", "respeto", "respect", "mvp", "grande", "golazo",
            "jugadon", "jugadón", "aplausos", "wholesome", "based");

    private static final Set<String> NEGATIVAS = Set.of(
            "malo", "mala", "horrible", "aburrido", "aburre", "trash", "basura",
            "cheater", "cheto", "hack", "hacker", "lag", "laggy", "rip", "ff",
            "noob", "manco", "peor", "odio", "hate", "boring", "scam", "robo",
            "fraude", "toxic", "toxico", "tóxico", "asco", "feo", "terrible",
            "pesimo", "pésimo", "caca", "injusto", "robaron", "lento", "muerto",
            "cringe", "buu", "abandonen", "malardo", "estafa", "decepcion",
            "decepción", "flopeo", "flop");

    // Emojis frecuentes con carga clara; los ambiguos (💀, 😂 en tono burla) se
    // dejan fuera a propósito para no meter ruido en la clasificación.
    //
    // Se guarda SOLO el punto de código base (sin U+FE0F). El mensaje se
    // normaliza antes de comparar, así que "❤" y "❤️" —literales distintos, y el
    // segundo es el que emiten teclados y Twitch— cuentan como un mismo emoji una
    // sola vez, en vez de sumar doble por estar ambas variantes en el set.
    private static final Set<String> EMOJIS_POSITIVOS = Set.of("🔥", "❤", "😍", "👍", "🎉", "💪");
    private static final Set<String> EMOJIS_NEGATIVOS = Set.of("😡", "👎", "🤬", "😤");

    /**
     * Selector de presentación emoji (U+FE0F). No aporta significado: solo pide
     * que el glifo se dibuje a color. Se escribe escapado porque es invisible en
     * el fuente y de otro modo nadie ve por qué está.
     */
    private static final String SELECTOR_VARIACION = "\uFE0F";

    @Override
    public Resultado analizar(List<String> mensajes) {
        int positivos = 0;
        int negativos = 0;

        for (String mensaje : mensajes) {
            if (mensaje == null || mensaje.isBlank()) {
                continue;
            }
            String texto = mensaje.toLowerCase(Locale.ROOT);
            var matcher = TOKEN.matcher(texto);
            while (matcher.find()) {
                String token = matcher.group();
                if (POSITIVAS.contains(token)) {
                    positivos++;
                } else if (NEGATIVAS.contains(token)) {
                    negativos++;
                }
            }
            // Los emojis se cuentan por ocurrencia, igual que las palabras: son
            // las dos señales del mismo puntaje y ponderarlas distinto (booleano
            // por mensaje vs. por ocurrencia) hacía que "🔥🔥🔥🔥🔥" pesara 1 y
            // "gg gg gg gg gg" pesara 5.
            String emojis = mensaje.replace(SELECTOR_VARIACION, "");
            for (String emoji : EMOJIS_POSITIVOS) {
                positivos += contarOcurrencias(emojis, emoji);
            }
            for (String emoji : EMOJIS_NEGATIVOS) {
                negativos += contarOcurrencias(emojis, emoji);
            }
        }

        int total = positivos + negativos;
        BigDecimal puntaje = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100L * (positivos - negativos))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new Resultado(ClasificacionSentimiento.desdePuntaje(puntaje), puntaje);
    }

    /** Ocurrencias no solapadas de {@code aguja} en {@code texto}. */
    private static int contarOcurrencias(String texto, String aguja) {
        int total = 0;
        int desde = 0;
        int i;
        while ((i = texto.indexOf(aguja, desde)) >= 0) {
            total++;
            desde = i + aguja.length();
        }
        return total;
    }
}
