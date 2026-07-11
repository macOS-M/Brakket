package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de disolución de un equipo (RF-03).
 * La ERS exige confirmación explícita; el motivo es opcional.
 */
public record DisolverEquipoRequest(

        @AssertTrue(message = "La confirmación explícita es obligatoria para disolver el equipo")
        boolean confirmacion,

        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo
) {
}
