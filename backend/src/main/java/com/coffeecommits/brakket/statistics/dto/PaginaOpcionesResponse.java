package com.coffeecommits.brakket.statistics.dto;

import java.util.List;

public record PaginaOpcionesResponse(List<OpcionEstadisticaResponse> items,
                                     int pagina,
                                     int tamano,
                                     long total) {}
