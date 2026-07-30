package com.coffeecommits.brakket.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImpugnarResultadoRequest(

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
        String descripcion,

        @Size(max = 500)
        String evidenciaUrl
) {
}