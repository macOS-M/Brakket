package com.coffeecommits.brakket.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Utilidades de fecha/hora comunes. Brakket trabaja en UTC internamente.
 */
public final class DateUtils {

    private DateUtils() { }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** true si {@code fin} no es anterior a {@code inicio} (rango válido). */
    public static boolean isValidRange(LocalDate inicio, LocalDate fin) {
        return inicio != null && fin != null && !fin.isBefore(inicio);
    }
}
