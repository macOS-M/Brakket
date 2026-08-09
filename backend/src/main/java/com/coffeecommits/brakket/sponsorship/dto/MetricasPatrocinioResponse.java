package com.coffeecommits.brakket.sponsorship.dto;

public record MetricasPatrocinioResponse(
        Long patrocinioId,
        Long transmisionId,
        Integer espectadoresPromedio,
        Integer picoEspectadores,
        Double mensajesPorMinutoPromedio,
        String sentimientoPredominante,
        boolean sentimientoPendiente
) {
}