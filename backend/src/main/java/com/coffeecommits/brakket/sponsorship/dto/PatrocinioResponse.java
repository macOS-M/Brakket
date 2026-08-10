package com.coffeecommits.brakket.sponsorship.dto;

import com.coffeecommits.brakket.sponsorship.model.Patrocinio;

import java.time.LocalDate;

public record PatrocinioResponse(
        Long id,
        Long patrocinadorId,
        String patrocinadorNombre,
        Long ligaId,
        Long temporadaId,
        Long torneoId,
        String nivel,
        String condiciones,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {

    public static PatrocinioResponse fromEntity(Patrocinio p) {
        return new PatrocinioResponse(
                p.getId(),
                p.getPatrocinador().getId(),
                p.getPatrocinador().getNombre(),
                p.getLiga() != null ? p.getLiga().getId() : null,
                p.getTemporada() != null ? p.getTemporada().getId() : null,
                p.getTorneo() != null ? p.getTorneo().getId() : null,
                p.getNivel(),
                p.getCondiciones(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getEstado()
        );
    }
}