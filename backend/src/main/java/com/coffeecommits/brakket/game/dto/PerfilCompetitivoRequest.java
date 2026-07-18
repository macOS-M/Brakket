package com.coffeecommits.brakket.game.dto;

import com.coffeecommits.brakket.game.model.ModalidadCompetitiva;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PerfilCompetitivoRequest(

        @NotNull
        @Positive
        Long juegoId,

        @NotNull
        ModalidadCompetitiva modalidad,

        @NotNull
        @Positive
        Integer plantillaMinima,

        @NotNull
        @Positive
        Integer plantillaMaxima,

        @NotEmpty
        List<Long> formatosIds,

        @NotEmpty
        List<Long> estadisticasIds

) {
}
