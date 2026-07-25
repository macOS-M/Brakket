package com.coffeecommits.brakket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas de la plataforma. Hoy la única es el
 * muestreo de audiencia de RF-36; cada tarea se protege sola (no corre sin
 * credenciales), así que en test/CI el scheduler existe pero no hace nada.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
