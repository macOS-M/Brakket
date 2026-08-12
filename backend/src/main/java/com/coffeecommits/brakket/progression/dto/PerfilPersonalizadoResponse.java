package com.coffeecommits.brakket.progression.dto;

import java.util.List;

/** Personalización visible públicamente; nunca incluye datos privados de la cuenta. */
public record PerfilPersonalizadoResponse(
        Long jugadorId,
        String nombre,
        ElementoAplicado titulo,
        List<ElementoAplicado> insignias) {

    public record ElementoAplicado(Long id, String nombre, String descripcion) {}
}
