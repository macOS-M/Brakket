package com.coffeecommits.brakket.tournament.dto;

import com.coffeecommits.brakket.tournament.model.TipoCasoEspecial;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarCasoEspecialRequest(

        @NotNull(message = "El tipo de caso especial es obligatorio")
        TipoCasoEspecial tipo,

        @Size(max = 500, message = "La justificacion no puede superar los 500 caracteres")
        String justificacion,

        @Size(max = 500)
        String evidenciaUrl,

        Long equipoGanadorId
) {
}