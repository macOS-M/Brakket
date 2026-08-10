package com.coffeecommits.brakket.analytics.dto;

import com.coffeecommits.brakket.common.exception.BusinessException;

import java.util.Locale;

/** RF-37: cómo se agregan las muestras de una serie antes de devolverlas. */
public enum AgrupacionMetrica {
    /** Una muestra, un punto. */
    CRUDA,
    /** Las muestras de cada hora se promedian en un punto. */
    HORA;

    /**
     * Se parsea acá y no en la firma del controller a propósito: un valor
     * inválido en un @RequestParam tipado lanza MethodArgumentTypeMismatchException,
     * que GlobalExceptionHandler no maneja y terminaría en un 500.
     */
    public static AgrupacionMetrica desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return CRUDA;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("La agrupacion no es valida: " + valor);
        }
    }
}
