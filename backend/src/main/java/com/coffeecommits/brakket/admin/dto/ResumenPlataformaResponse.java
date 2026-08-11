package com.coffeecommits.brakket.admin.dto;

/**
 * Conteos globales de la plataforma para el panel de administración (RF-49,
 * EPIC-14). Cada campo es una métrica de estado que el ADMIN supervisa de un
 * vistazo.
 */
public record ResumenPlataformaResponse(
        long usuarios,
        long usuariosBloqueados,
        long equipos,
        long juegos,
        long juegosActivos,
        long ligas,
        long torneos,
        long torneosEnCurso,
        long transmisionesActivas,
        long disputas
) {
}
