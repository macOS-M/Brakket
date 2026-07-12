package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Traduce los roles y permisos del usuario (BD) a authorities de Spring
 * Security. Es lo que hace funcionar @PreAuthorize en toda la aplicación.
 */
@Service
@RequiredArgsConstructor
public class AutoridadesService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    /**
     * Authorities del usuario identificado por correo.
     * - Optional vacío → NO autenticar (usuario inexistente o cuenta
     *   bloqueada; criterio RF-19 de cuenta bloqueada).
     * - Usuario sin roles → autoridad mínima ROLE_SIN_ROL (criterio RF-19:
     *   acceso limitado hasta completar la configuración).
     */
    @Transactional(readOnly = true)
    public Optional<List<GrantedAuthority>> cargarAutoridades(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
        if (usuarioOpt.isEmpty() || Boolean.TRUE.equals(usuarioOpt.get().getBloqueado())) {
            return Optional.empty();
        }

        Set<GrantedAuthority> autoridades = new LinkedHashSet<>();

        usuarioRolRepository.findByUsuarioId(usuarioOpt.get().getId()).forEach(ur -> {
            autoridades.add(new SimpleGrantedAuthority(
                    "ROLE_" + ur.getRol().getNombreRol().toUpperCase()));
            ur.getRol().getPermisos().forEach(p ->
                    autoridades.add(new SimpleGrantedAuthority(p.getCodigo())));
        });

        if (autoridades.isEmpty()) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_SIN_ROL"));
        }

        return Optional.of(List.copyOf(autoridades));
    }
}