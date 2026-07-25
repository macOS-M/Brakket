package com.coffeecommits.brakket.team.event;

import com.coffeecommits.brakket.auth.model.Usuario;

/**
 * RF-10: se publica cuando un integrante fue expulsado y la baja ya quedó
 * persistida. La notificación se envía al recibir este evento, después del
 * commit, para que un fallo al notificar no pueda revertir la baja.
 */
public record IntegranteExpulsadoEvent(
        Long miembroId,
        Usuario destinatario,
        String equipoNombre,
        String causa
) {
}
