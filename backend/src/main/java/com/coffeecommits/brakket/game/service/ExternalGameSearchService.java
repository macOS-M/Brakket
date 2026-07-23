package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.game.dto.JuegoExternoResponse;

import java.util.List;

/**
 * Búsqueda de juegos en un catálogo externo (RAWG) para precargar el
 * formulario del catálogo con datos y arte reales en lugar de tipearlos.
 */
public interface ExternalGameSearchService {

    /** Busca juegos por texto libre. Lista vacía si no hay coincidencias. */
    List<JuegoExternoResponse> buscar(String consulta);

    /** Los títulos más populares del catálogo externo (para sembrar el propio). */
    List<JuegoExternoResponse> populares();

    /** Hay credenciales configuradas para consultar el catálogo externo. */
    boolean disponible();
}
