package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.dto.PerfilUsuarioRequest;
import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.model.Rol;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import com.coffeecommits.brakket.auth.model.VisibilidadPerfil;
import com.coffeecommits.brakket.auth.repository.RolRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    /** Rol base que recibe todo usuario nuevo al autenticarse (sembrado en la migración V2). */
    private static final String ROL_BASE = "JUGADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final JuegoRepository juegoRepository;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           UsuarioRolRepository usuarioRolRepository,
                           JuegoRepository juegoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.juegoRepository = juegoRepository;
    }

    @Override
    @Transactional
    public Usuario upsertGoogleUser(String googleId, String correo, String nombre, String fotoUrl) {
        Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                .map(existing -> {
                    existing.setCorreo(correo);
                    if (esTextoVacio(existing.getNombre())) {
                        existing.setNombre(nombre);
                    }
                    if (esTextoVacio(existing.getFotoUrl())) {
                        existing.setFotoUrl(fotoUrl);
                    }
                    return existing;
                })
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .googleId(googleId)
                        .correo(correo)
                        .nombre(nombre)
                        .fotoUrl(fotoUrl)
                        .visibilidadPerfil(VisibilidadPerfil.PUBLIC)
                        .build()));

        asegurarRolBase(usuario);
        return usuario;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse getCurrentUser(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));
        return responseDe(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse updateCurrentUser(String correo, PerfilUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", correo));

        usuario.setNombre(request.nombre().trim());
        usuario.setFotoUrl(request.foto().trim());
        usuario.setBiografia(request.biografia().trim());
        usuario.setRedesSociales(request.redesSociales() == null ? null : request.redesSociales().trim());
        usuario.setVisibilidadPerfil(request.visibilidadPerfil());

        if (request.juegoIds() != null) {
            Set<Long> juegoIds = new LinkedHashSet<>(request.juegoIds());
            List<Juego> juegos = juegoRepository.findAllById(juegoIds);
            if (juegos.size() != juegoIds.size()) {
                throw new ResourceNotFoundException("Juego", "uno o más juegos preferidos");
            }
            usuario.getJuegosPreferidos().clear();
            usuario.getJuegosPreferidos().addAll(juegos);
        }

        usuarioRepository.save(usuario);
        return responseDe(usuario);
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

    private UsuarioResponse responseDe(Usuario usuario) {
        List<String> roles = rolesDe(usuario.getId());
        List<Long> juegoIds = usuario.getJuegosPreferidos().stream()
                .map(Juego::getId)
                .toList();
        return new UsuarioResponse(true, usuario.getId(), usuario.getNombre(),
                usuario.getCorreo(), usuario.getFotoUrl(), usuario.getBiografia(),
                usuario.getRedesSociales(), usuario.getVisibilidadPerfil() == null ? null : usuario.getVisibilidadPerfil().name(),
                juegoIds, roles);
    }

    private boolean esTextoVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
