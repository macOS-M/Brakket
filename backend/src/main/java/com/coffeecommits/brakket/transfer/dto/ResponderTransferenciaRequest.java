package com.coffeecommits.brakket.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Respuesta de una parte autorizada a una solicitud de transferencia (RF-13).
 */
public record ResponderTransferenciaRequest(
        @NotNull(message = "La decisión es obligatoria")
        @Pattern(regexp = "ACEPTAR|RECHAZAR", message = "La decisión debe ser ACEPTAR o RECHAZAR")
        String decision
) {

    public boolean esRechazo() {
        return "RECHAZAR".equals(decision);
    }
}
