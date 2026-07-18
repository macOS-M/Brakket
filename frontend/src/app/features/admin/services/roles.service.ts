import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { AsignarRolRequest, RolDTO, UsuarioRolesDTO } from '../../../models/rol.model';

/**
 * Servicio de datos de la feature "admin" — control de roles (RF-19).
 */
@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly api = inject(ApiService);

  /** Catálogo de roles reconocidos por el sistema. */
  listarCatalogo(): Observable<RolDTO[]> {
    return this.api.get<RolDTO[]>('/admin/roles');
  }

  /** Roles y permisos del usuario autenticado. */
  misPermisos(): Observable<UsuarioRolesDTO> {
    return this.api.get<UsuarioRolesDTO>('/admin/usuarios/mis-permisos');
  }

  /** Roles y permisos de un usuario puntual (requiere GESTIONAR_ROLES). */
  permisosDeUsuario(usuarioId: number): Observable<UsuarioRolesDTO> {
    return this.api.get<UsuarioRolesDTO>(`/admin/usuarios/${usuarioId}/permisos`);
  }

  /** Asigna un rol del catálogo a un usuario. */
  asignarRol(usuarioId: number, rolId: number): Observable<UsuarioRolesDTO> {
    const body: AsignarRolRequest = { rolId };
    return this.api.post<UsuarioRolesDTO>(`/admin/usuarios/${usuarioId}/roles`, body);
  }

  /** Revoca un rol previamente asignado a un usuario. */
  revocarRol(usuarioId: number, rolId: number): Observable<UsuarioRolesDTO> {
    return this.api.delete<UsuarioRolesDTO>(`/admin/usuarios/${usuarioId}/roles/${rolId}`);
  }
}
