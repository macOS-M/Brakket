package com.coffeecommits.brakket.progression.dto;

/** Personalización visible públicamente; nunca incluye datos privados de la cuenta. */
public record PerfilPersonalizadoResponse(
        Long jugadorId,
        String nombre,
        ElementoAplicado titulo,
        ElementoAplicado insignia) {

    public record ElementoAplicado(Long id, String nombre, String descripcion) {}
}
