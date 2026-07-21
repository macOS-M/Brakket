package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.team.dto.EquipoBusquedaResponse;

/**
 * Búsqueda de equipos (RF-05, EPIC-02).
 */
public interface TeamSearchService {

    /**
     * Busca equipos aplicando los filtros recibidos; todos son opcionales.
     *
     * @param texto      texto a buscar dentro del nombre del equipo
     * @param juegoId    filtra por juego específico
     * @param disciplina filtra por disciplina (género del juego)
     * @param estado     filtra por estado del equipo (ACTIVO o DISUELTO)
     * @param page       número de página (base 0)
     * @param size       tamaño de página
     */
    PageResponse<EquipoBusquedaResponse> buscar(String texto,
                                                Long juegoId,
                                                String disciplina,
                                                String estado,
                                                int page,
                                                int size);
}
