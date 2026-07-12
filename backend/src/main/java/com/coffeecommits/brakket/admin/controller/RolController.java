package com.coffeecommits.brakket.admin.controller;

import com.coffeecommits.brakket.admin.dto.AsignarRolRequest;
import com.coffeecommits.brakket.admin.dto.RolDTO;
import com.coffeecommits.brakket.admin.dto.UsuarioRolesDTO;
import com.coffeecommits.brakket.admin.service.RolPermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del RF-19 (Control de roles y permisos).
 * La protección de PERMISOS se declara con @PreAuthorize (lado servidor);
 * el front-end además oculta estas acciones en la interfaz, cumpliendo el
 * criterio de aplicar permisos en interfaz y servidor.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class RolController {

    private final RolPermisoService rolPermisoService;

    /**
     * Catálogo de roles reconocidos (ADMIN, COMISIONADO, ARBITRO, CAPITAN,
     * JUGADOR, PATROCINADOR). Cualquier autenticado puede consultarlo,
     * p. ej. para poblar el combo del panel administrativo.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RolDTO>> listarRoles() {
        return ResponseEntity.ok(rolPermisoService.listarCatalogo());
    }

    /**
     * Roles y permisos aplicados del usuario autenticado. Angular lo
     * consume tras el login para redirigir según rol y estado de perfil.
     */
    @GetMapping("/usuarios/mis-permisos")
    public ResponseEntity<UsuarioRolesDTO> misPermisos(Authentication authentication) {
        return ResponseEntity.ok(
                rolPermisoService.obtenerPermisosPorCorreo(authentication.getName()));
    }

    /** Roles y permisos de otro usuario. Solo para quien gestiona roles. */
    @GetMapping("/usuarios/{usuarioId}/permisos")
    @PreAuthorize("hasAuthority('GESTIONAR_ROLES')")
    public ResponseEntity<UsuarioRolesDTO> permisosDeUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(rolPermisoService.obtenerPermisosDeUsuario(usuarioId));
    }

    /** Asigna un rol del catálogo a un usuario. */
    @PostMapping("/usuarios/{usuarioId}/roles")
    @PreAuthorize("hasAuthority('GESTIONAR_ROLES')")
    public ResponseEntity<UsuarioRolesDTO> asignarRol(@PathVariable Long usuarioId,
                                                      @Valid @RequestBody AsignarRolRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(rolPermisoService.asignarRol(
                usuarioId, request.getRolId(), authentication.getName()));
    }

    /** Revoca un rol previamente asignado a un usuario. */
    @DeleteMapping("/usuarios/{usuarioId}/roles/{rolId}")
    @PreAuthorize("hasAuthority('GESTIONAR_ROLES')")
    public ResponseEntity<UsuarioRolesDTO> revocarRol(@PathVariable Long usuarioId,
                                                      @PathVariable Long rolId,
                                                      Authentication authentication) {
        return ResponseEntity.ok(rolPermisoService.revocarRol(
                usuarioId, rolId, authentication.getName()));
    }
}