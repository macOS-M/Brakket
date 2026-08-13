package com.coffeecommits.brakket.report.model;

import com.coffeecommits.brakket.common.exception.BusinessException;

import java.util.Locale;

/** RF-50: los 4 tipos de reporte exportables (ERS: "competencias, audiencia,
 * patrocinio, estadísticas o resultados" — competencias y resultados son
 * el mismo dato, ver decisión de alcance). */
public enum TipoReporte {
    COMPETENCIA,
    AUDIENCIA,
    PATROCINIO,
    ESTADISTICA;

    public static TipoReporte desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("Debe indicar el tipo de reporte.");
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("El tipo de reporte no es válido: " + valor);
        }
    }
}