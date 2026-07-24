package com.coffeecommits.brakket.twitch.model;

/**
 * Procedencia de una muestra de métricas (RF-36): el ERS exige poder
 * distinguir datos reales de Twitch de datos simulados para el contexto
 * académico. El muestreo automático solo escribe REAL.
 */
public enum OrigenMetrica {
    REAL, SIMULADO
}
