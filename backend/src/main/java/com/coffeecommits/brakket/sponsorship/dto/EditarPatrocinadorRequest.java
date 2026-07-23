package com.coffeecommits.brakket.sponsorship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record EditarPatrocinadorRequest(

        @NotBlank(message = "El nombre comercial es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @URL(message = "El logo debe ser una URL valida")
        String logo,

        @NotBlank(message = "El contacto es obligatorio")
        @Size(max = 180)
        String contacto,

        @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
        String descripcion,

        List<@URL(message = "El enlace debe tener formato de URL valido") String> enlaces
) {
}