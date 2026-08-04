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

    /** A partir de |puntaje| ≥ este umbral el sentimiento deja de ser NEUTRO. */
    private static final BigDecimal UMBRAL = new BigDecimal("20");

    /** Extrae palabras respetando acentos/ñ (letras y dígitos Unicode). */
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");

    private static final Set<String> POSITIVAS = Set.of(
            "gg", "ggwp", "pog", "poggers", "pogchamp", "hype", "hypee", "increible",
            "increíble", "excelente", "genial", "bueno", "buena", "buenardo", "win",
            "clutch", "ez", "limpio", "crack", "vamos", "vamooos", "amo", "love",
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
            "cringe", "wtf", "buu", "abandonen", "malardo", "estafa", "decepcion",
            "decepción", "flopeo", "flop");

    // Emojis frecuentes con carga clara; los ambiguos (💀, 😂 en tono burla) se
    // dejan fuera a propósito para no meter ruido en la clasificación.
    private static final Set<String> EMOJIS_POSITIVOS = Set.of("🔥", "❤", "❤️", "😍", "👍", "🎉", "💪");
    private static final Set<String> EMOJIS_NEGATIVOS = Set.of("😡", "👎", "🤬", "😤");

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
            for (String emoji : EMOJIS_POSITIVOS) {
                if (mensaje.contains(emoji)) {
                    positivos++;
                }
            }
            for (String emoji : EMOJIS_NEGATIVOS) {
                if (mensaje.contains(emoji)) {
                    negativos++;
                }
            }
        }

        int total = positivos + negativos;
        BigDecimal puntaje = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100L * (positivos - negativos))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new Resultado(clasificar(puntaje), puntaje);
    }

    private ClasificacionSentimiento clasificar(BigDecimal puntaje) {
        if (puntaje.compareTo(UMBRAL) >= 0) {
            return ClasificacionSentimiento.POSITIVO;
        }
        if (puntaje.compareTo(UMBRAL.negate()) <= 0) {
            return ClasificacionSentimiento.NEGATIVO;
        }
        return ClasificacionSentimiento.NEUTRO;
    }
}
