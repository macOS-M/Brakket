package com.coffeecommits.brakket.sponsorship.dto;

import java.time.LocalDate;
import java.util.List;

public record PanelComercialResponse(
        Long patrocinadorId,
        String patrocinadorNombre,
        List<PatrocinioResumen> patrocinios
) {
    public record PatrocinioResumen(
            Long patrocinioId,
            String nivel,
            String estado,
            boolean vencido,
            Long ligaId,
            Long temporadaId,
            Long torneoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            int cantidadEspacios
    ) {
    }
}