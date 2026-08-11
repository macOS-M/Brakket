package com.coffeecommits.brakket.team.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * RF-02: edición parcial de equipo. Todos los campos son opcionales;
 * si un campo viaja en null, el servicio no lo toca. Si viaja con un
 * valor, se valida y se aplica.
 */
public record EditarEquipoRequest(

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

        Long juegoId,

        List<Long> juegoIds,

        List<@URL(message = "El enlace debe tener formato de URL valido") String> redesSociales,

        @Pattern(regexp = "PUBLIC|PRIVATE", message = "El estado de privacidad debe ser PUBLIC o PRIVATE")
        String estadoPrivacidad,

        /**
         * Versión del equipo que el cliente leyó en el GET (control de
         * concurrencia optimista). Si al guardar difiere de la actual,
         * el servicio responde 409. Opcional para no romper clientes
         * que aún no la envían.
         */
        Long version
) {
    public EditarEquipoRequest(String nombre, String logo, String bannerUrl, String descripcion,
                               String sitioWeb, String videoUrl, Long juegoId,
                               List<String> redesSociales, String estadoPrivacidad, Long version) {
        this(nombre, logo, bannerUrl, descripcion, sitioWeb, videoUrl, juegoId, null,
                redesSociales, estadoPrivacidad, version);
    }
}
