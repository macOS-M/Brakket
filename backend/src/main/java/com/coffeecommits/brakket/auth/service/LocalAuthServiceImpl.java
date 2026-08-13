package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.auth.dto.LoginLocalRequest;
import com.coffeecommits.brakket.auth.dto.RegistroLocalRequest;
import com.coffeecommits.brakket.auth.model.Rol;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import com.coffeecommits.brakket.auth.model.VisibilidadPerfil;
import com.coffeecommits.brakket.auth.repository.RolRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.config.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

/**
 * Cuentas locales con BCrypt. Nota de seguridad: el bootstrap de ADMIN por
 * correo (brakket.admin-emails) aplica SOLO al login con Google, donde el
 * correo llega verificado; registrarse localmente con un correo del equipo
 * no otorga ningún rol especial.
 */
@Service
public class LocalAuthServiceImpl implements LocalAuthService {

    private static final String ROL_BASE = "JUGADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final JwtService jwtService;
    private final IntentosLoginService intentosLoginService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LocalAuthServiceImpl(UsuarioRepository usuarioRepository,
                                RolRepository rolRepository,
                                UsuarioRolRepository usuarioRolRepository,
                                JwtService jwtService,
                                IntentosLoginService intentosLoginService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.jwtService = jwtService;
        this.intentosLoginService = intentosLoginService;
    }

    @Override
    @Transactional
    public String registrar(RegistroLocalRequest request) {
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new BusinessException(
                    "Ya existe una cuenta con ese correo (si es tuya, probá iniciar sesión)");
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .nombre(request.nombre().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .visibilidadPerfil(VisibilidadPerfil.PUBLIC)
                .build());

        Rol rolBase = rolRepository.findByNombreRol(ROL_BASE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rol base '%s' no existe (revisar migración V2)".formatted(ROL_BASE)));
        usuarioRolRepository.save(UsuarioRol.builder().usuario(usuario).rol(rolBase).build());

        return emitirToken(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public String login(LoginLocalRequest request) {
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        // El freno va antes que todo: bloqueado no se consulta la BD ni se
        // compara BCrypt, así el martilleo no cuesta nada al servidor.
        intentosLoginService.verificarBloqueo(correo);

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> falloDeLogin(correo));

        if (usuario.getPasswordHash() == null) {
            throw new BusinessException("Esa cuenta inicia sesión con Google");
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw falloDeLogin(correo);
        }
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            throw new ForbiddenException("La cuenta está bloqueada");
        }

        intentosLoginService.registrarExito(correo);
        return emitirToken(usuario);
    }

    private String emitirToken(Usuario usuario) {
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre();
        return jwtService.generateToken(usuario.getCorreo(), Map.of("name", nombre));
    }

    /**
     * Mensaje único: no revela si falló el correo o la contraseña. El fallo
     * se cuenta también para correos inexistentes — si solo contaran los
     * reales, el freno mismo delataría qué correos tienen cuenta.
     */
    private BusinessException falloDeLogin(String correo) {
        intentosLoginService.registrarFallo(correo);
        return new BusinessException("Correo o contraseña incorrectos");
    }
}
