package com.coffeecommits.brakket.admin.dto;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Getter
@Builder
public class UsuarioRolesDTO {

    private Long usuarioId;
    private String correo;
    private List<String> roles;
    private List<String> permisos;

    public static UsuarioRolesDTO desde(Usuario usuario, List<UsuarioRol> asignaciones) {
        List<String> roles = asignaciones.stream()
                .map(ur -> ur.getRol().getNombreRol())
                .sorted()
                .toList();

        // Unión de permisos de todos los roles, sin duplicados y ordenada
        Set<String> permisos = new TreeSet<>();
        asignaciones.forEach(ur -> ur.getRol().getPermisos()
                .forEach(p -> permisos.add(p.getCodigo())));

        return UsuarioRolesDTO.builder()
                .usuarioId(usuario.getId())
                .correo(usuario.getCorreo())
                .roles(roles)
                .permisos(List.copyOf(permisos))
                .build();
    }
}