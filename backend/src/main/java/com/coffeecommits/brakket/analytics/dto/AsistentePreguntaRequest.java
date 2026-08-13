package com.coffeecommits.brakket.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pregunta del administrador al asistente del termómetro (RF-40, EPIC-10).
 *
 * <p>El tope de largo no es cosmético: la pregunta es el único texto que un
 * humano inyecta en el prompt, así que acotarla limita tanto el gasto de tokens
 * como el margen para intentar reescribir las instrucciones del sistema.</p>
 */
public record AsistentePreguntaRequest(

        @NotBlank(message = "Escribí una pregunta.")
        @Size(max = 500, message = "La pregunta no puede superar los 500 caracteres.")
        String pregunta) {
}
