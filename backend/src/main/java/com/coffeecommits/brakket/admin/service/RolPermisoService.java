package com.coffeecommits.brakket.admin.service;

import com.coffeecommits.brakket.admin.dto.RolDTO;
import com.coffeecommits.brakket.admin.dto.UsuarioRolesDTO;
import com.coffeecommits.brakket.admin.model.LogAuditoria;
import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Rol;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.UsuarioRol;
import com.coffeecommits.brakket.auth.repository.RolRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.JerarquiaInvalidaException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas de negocio del RF-19 (Control de roles y permisos).
 * Las validaciones de PERMISO (quién puede invocar esto) las hace
 * Spring Security con @PreAuthorize en el controlador; aquí se validan
 * las reglas de negocio: catálogo, jerarquía, estado de cuenta y auditoría.
 */
@Service
@RequiredArgsConstructor
public class RolPermisoService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;

    /** Catálogo de roles reconocidos por el sistema (criterio RF-19). */
    public List<RolDTO> listarCatalogo() {
        return rolRepository.findAll().stream().map(RolDTO::desde).toList();
    }

    /** Roles y permisos aplicados de un usuario (dato de salida de RF-19). */
    @Transactional(readOnly = true)
    public UsuarioRolesDTO obtenerPermisosDeUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
        return UsuarioRolesDTO.desde(usuario, usuarioRolRepository.findByUsuarioId(usuarioId));
    }

    /** Versión por correo, para el endpoint "mis permisos" del usuario autenticado. */
    @Transactional(readOnly = true)
    public UsuarioRolesDTO obtenerPermisosPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario con el correo " + correo));
        return UsuarioRolesDTO.desde(usuario, usuarioRolRepository.findByUsuarioId(usuario.getId()));
    }

    /**
     * Asigna un rol a un usuario. Validaciones RF-19:
     *  1. El rol debe existir en el catálogo (si no existe, no se guarda nada).
     *  2. El actor no puede otorgar un rol con más jerarquía que la suya
     *     (esto también le impide elevarse a sí mismo).
     *  3. La cuenta destino no debe estar bloqueada.
     *  4. El cambio queda registrado en el log de auditoría.
     */
    @Transactional
    public UsuarioRolesDTO asignarRol(Long usuarioObjetivoId, Long rolId, String correoActor) {

        // (1) El rol debe existir en el catálogo
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));

        // (2) Jerarquía del actor vs. jerarquía del rol a otorgar
        Usuario actor = validarJerarquia(correoActor, rol, "asignar");

        Usuario objetivo = usuarioRepository.findById(usuarioObjetivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioObjetivoId));

        // (3) La cuenta destino no debe estar bloqueada
        if (Boolean.TRUE.equals(objetivo.getBloqueado())) {
            throw new BusinessException("La cuenta destino está bloqueada; no se pueden modificar sus roles.");
        }

        // No duplicar la asignación
        if (usuarioRolRepository.existsByUsuarioIdAndRolId(usuarioObjetivoId, rolId)) {
            throw new BusinessException("El usuario ya tiene asignado el rol " + rol.getNombreRol() + ".");
        }

        usuarioRolRepository.save(UsuarioRol.builder()
                .usuario(objetivo)
                .rol(rol)
                .build());

        // (4) Auditoría
        registrarAuditoria(actor, "ROL_ASIGNADO: " + rol.getNombreRol(), objetivo.getId());

        return obtenerPermisosDeUsuario(usuarioObjetivoId);
    }

    /** Revoca un rol de un usuario, con las mismas garantías que la asignación. */
    @Transactional
    public UsuarioRolesDTO revocarRol(Long usuarioObjetivoId, Long rolId, String correoActor) {

        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));

        Usuario actor = validarJerarquia(correoActor, rol, "revocar");

        UsuarioRol asignacion = usuarioRolRepository
                .findByUsuarioIdAndRolId(usuarioObjetivoId, rolId)
                .orElseThrow(() -> new BusinessException(
                        "El usuario no tiene asignado el rol " + rol.getNombreRol() + "."));

        usuarioRolRepository.delete(asignacion);

        registrarAuditoria(actor, "ROL_REVOCADO: " + rol.getNombreRol(), usuarioObjetivoId);

        return obtenerPermisosDeUsuario(usuarioObjetivoId);
    }

    // ------------------- métodos privados de apoyo -------------------

    /**
     * Valida la jerarquía y devuelve al actor (se reutiliza para auditar).
     * Nivel efectivo del actor = el mejor (menor número) de todos sus roles.
     * Regla: no se puede asignar/revocar un rol de jerarquía superior
     * (nivel menor) a la propia. Un actor sin roles no puede administrar roles.
     */
    private Usuario validarJerarquia(String correoActor, Rol rolAfectado, String accion) {
        Usuario actor = usuarioRepository.findByCorreo(correoActor)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se pudo identificar al usuario que realiza el cambio."));

        Integer nivelActor = usuarioRolRepository.findByUsuarioId(actor.getId()).stream()
                .map(ur -> ur.getRol().getNivel())
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new JerarquiaInvalidaException(
                        "Su cuenta no tiene un rol asignado, por lo que no puede administrar roles."));

        if (rolAfectado.getNivel() < nivelActor) {
            throw new JerarquiaInvalidaException(
                    "No puede " + accion + " un rol con más privilegios que el suyo.");
        }
        return actor;
    }

    /** Registra el cambio en log_auditoria (tabla del esquema V1, RNF-04/RNF-14). */
    private void registrarAuditoria(Usuario actor, String accion, Long usuarioObjetivoId) {
        logAuditoriaRepository.save(LogAuditoria.builder()
                .usuario(actor)
                .accion(accion)
                .entidad("usuario")
                .entidadId(usuarioObjetivoId)
                .fecha(LocalDateTime.now())
                .build());
    }
}