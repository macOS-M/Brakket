package com.coffeecommits.brakket.statistics.dto;

import java.util.List;

public record CatalogoEstadisticasResponse(List<OpcionEstadisticaResponse> ligas,
                                           List<OpcionEstadisticaResponse> temporadas) {}
