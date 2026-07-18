package com.coffeecommits.brakket.admin.dto;

import com.coffeecommits.brakket.auth.model.Rol;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RolDTO {

    private Long id;
    private String nombreRol;
    private Integer nivel;
    private List<String> permisos;

    public static RolDTO desde(Rol rol) {
        return RolDTO.builder()
                .id(rol.getId())
                .nombreRol(rol.getNombreRol())
                .nivel(rol.getNivel())
                .permisos(rol.getPermisos().stream()
                        .map(p -> p.getCodigo())
                        .sorted()
                        .toList())
                .build();
    }
}