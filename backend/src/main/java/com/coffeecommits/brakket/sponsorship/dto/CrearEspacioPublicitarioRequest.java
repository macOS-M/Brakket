package com.coffeecommits.brakket.sponsorship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CrearEspacioPublicitarioRequest(

        @NotNull(message = "El patrocinio es obligatorio")
        Long patrocinioId,

        @NotBlank(message = "La ubicación es obligatoria")
        String ubicacion,

        @NotBlank(message = "La imagen es obligatoria")
        @URL(message = "La imagen debe ser una URL válida")
        @Size(max = 500, message = "La URL de la imagen no puede superar los 500 caracteres")
        String imagenUrl,

        @URL(message = "El enlace debe ser una URL válida")
        @Size(max = 500, message = "El enlace no puede superar los 500 caracteres")
        String enlaceUrl
) {
}