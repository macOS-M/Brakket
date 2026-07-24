package com.coffeecommits.brakket.sponsorship.dto;

import com.coffeecommits.brakket.sponsorship.model.Patrocinador;

import java.util.List;

public record PatrocinadorResponse(
        Long id,
        String nombre,
        String logo,
        String contacto,
        String descripcion,
        String estado,
        List<String> enlaces
) {

    public static PatrocinadorResponse fromEntity(Patrocinador p, List<String> enlaces) {
        return new PatrocinadorResponse(
                p.getId(),
                p.getNombre(),
                p.getLogo(),
                p.getContacto(),
                p.getDescripcion(),
                p.getEstado(),
                enlaces
        );
    }
}