package com.coffeecommits.brakket.sponsorship.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CrearPatrocinioRequest(

        @NotNull(message = "El patrocinador es obligatorio")
        Long patrocinadorId,

        Long ligaId,

        Long temporadaId,

        Long torneoId,

        @Size(max = 500, message = "Las condiciones no pueden superar los 500 caracteres")
        String condiciones,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin
) {
}