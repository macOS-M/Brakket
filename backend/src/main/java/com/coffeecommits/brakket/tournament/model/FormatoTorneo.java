package com.coffeecommits.brakket.tournament.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatos con motor de competencia real (cierra la deuda DD-05). El campo
 * {@code torneo.formato} sigue siendo texto por compatibilidad con los
 * torneos ya creados ("Eliminación directa"); este enum lo interpreta de
 * forma laxa y define el código canónico que se guarda de ahora en adelante.
 */
public enum FormatoTorneo {
    ELIMINACION_DIRECTA,
    DOBLE_ELIMINACION,
    ROUND_ROBIN,
    SUIZO,
    FASE_GRUPOS_Y_ELIMINACION;

    /**
     * Interpretación laxa: acepta el código del catálogo, el nombre legible
     * con acentos o variantes con espacios. Vacío si no se reconoce.
     */
    public static Optional<FormatoTorneo> interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String plano = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        // El orden importa: las palabras más específicas mandan. GRUPO y
        // DOBLE contienen "ELIMINACION", y un futuro "round robin doble"
        // (liga a dos vueltas) debe caer en ROBIN, no en doble eliminación.
        if (plano.contains("GRUPO")) {
            return Optional.of(FASE_GRUPOS_Y_ELIMINACION);
        }
        if (plano.contains("ROBIN")) {
            return Optional.of(ROUND_ROBIN);
        }
        if (plano.contains("SUIZO") || plano.contains("SWISS")) {
            return Optional.of(SUIZO);
        }
        if (plano.contains("DOBLE")) {
            return Optional.of(DOBLE_ELIMINACION);
        }
        if (plano.contains("ELIMINACION") || plano.contains("DIRECTA")) {
            return Optional.of(ELIMINACION_DIRECTA);
        }
        return Optional.empty();
    }

    /** Para el motor: un formato desconocido (dato viejo) corre como eliminación directa. */
    public static FormatoTorneo de(Torneo torneo) {
        return interpretar(torneo.getFormato()).orElse(ELIMINACION_DIRECTA);
    }
}
