package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record CrearEquipoRequest(

        @NotBlank(message = "El nombre del equipo es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @URL(message = "El logo debe ser una URL valida")
        String logo,

        @URL(message = "El banner debe ser una URL valida")
        String bannerUrl,

        @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
        String descripcion,

        @URL(message = "El sitio web debe ser una URL valida")
        String sitioWeb,

        @URL(message = "El video debe ser una URL valida")
        String videoUrl,

        @NotNull(message = "El juego principal es obligatorio")
        Long juegoId,

        List<@URL(message = "El enlace debe tener formato de URL valido") String> redesSociales,

        /**
         * Correo del jugador que será capitán. Obligatorio cuando quien crea
         * es un ADMIN: los administradores arman el equipo para otros
         * jugadores, no forman parte de él. Los jugadores lo dejan null.
         */
        @Size(max = 254, message = "El correo del capitán no puede superar los 254 caracteres")
        String capitanCorreo
) {
}