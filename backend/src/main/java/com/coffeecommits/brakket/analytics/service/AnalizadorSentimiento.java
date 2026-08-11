package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;

import java.math.BigDecimal;
import java.util.List;

/**
 * Motor de análisis de sentimiento del chat (RF-39, EPIC-10).
 *
 * <p>Abstrae <b>cómo</b> se clasifica el sentimiento para cumplir RNF-23: hoy el
 * proyecto usa una implementación léxica determinista ({@link AnalizadorLexico})
 * que no necesita servicios externos y funciona en la demo; mañana un proveedor
 * de IA real ({@code brakket.ai.*}) puede enchufarse por esta misma interfaz sin
 * tocar el servicio ni el controlador.</p>
 */
public interface AnalizadorSentimiento {

    /**
     * Clasifica el sentimiento agregado de un lote de mensajes de chat.
     *
     * @param mensajes mensajes de chat (no nulos, ya filtrados de vacíos)
     * @return clasificación y puntaje en el rango [-100, 100]
     */
    Resultado analizar(List<String> mensajes);

    /** Resultado del análisis: clasificación cualitativa + puntaje [-100, 100]. */
    record Resultado(ClasificacionSentimiento clasificacion, BigDecimal puntaje) {
    }
}
