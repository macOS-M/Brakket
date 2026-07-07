package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.model.Rol;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import com.coffeecommits.brakket.auth.repository.RolRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    /** Rol base que recibe todo usuario nuevo al autenticarse (sembrado en la migración V2). */
    private static final String ROL_BASE = "JUGADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           UsuarioRolRepository usuarioRolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
    }

    @Override
    @Transactional
    public Usuario upsertGoogleUser(String googleId, String correo, String nombre, String fotoUrl) {
        Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                .map(existing -> {
                    existing.setNombre(nombre);
                    existing.setCorreo(correo);
                    existing.setFotoUrl(fotoUrl);
                    return existing;
                })
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .googleId(googleId)
                        .correo(correo)
                        .nombre(nombre)
                        .fotoUrl(fotoUrl)
                        .build()));

        asegurarRolBase(usuario);
        return usuario;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse getCurrentUser(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        List<String> roles = rolesDe(usuario.getId());
        return new UsuarioResponse(true, usuario.getId(), usuario.getNombre(),
                usuario.getCorreo(), usuario.getFotoUrl(), roles);
    }

    /** Asigna el rol base al usuario si aún no lo tiene (idempotente). */
    private void asegurarRolBase(Usuario usuario) {
        boolean yaLoTiene = usuarioRolRepository.findByUsuarioId(usuario.getId()).stream()
                .anyMatch(ur -> ROL_BASE.equals(ur.getRol().getNombreRol()));
        if (yaLoTiene) {
            return;
        }
        Rol rolBase = rolRepository.findByNombreRol(ROL_BASE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rol base '%s' no existe (revisar migración V2)".formatted(ROL_BASE)));
        usuarioRolRepository.save(UsuarioRol.builder().usuario(usuario).rol(rolBase).build());
    }

    private List<String> rolesDe(Long usuarioId) {
        return usuarioRolRepository.findByUsuarioId(usuarioId).stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();
    }
}
