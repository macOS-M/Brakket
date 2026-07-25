package com.coffeecommits.brakket.league.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ActualizarTemporadaRequest(
        @Valid @NotNull CrearTemporadaRequest configuracion,
        @NotNull(message = "La version es obligatoria") Long version
) {}
