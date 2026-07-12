import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { RolesService } from '../../services/roles.service';
import { ApiErrorBody, RolDTO, UsuarioRolesDTO } from '../../../../models/rol.model';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-panel.component.html',
  styleUrl: './admin-panel.component.scss'
})
export class AdminPanelComponent implements OnInit {
  private readonly rolesService = inject(RolesService);

  catalogo: RolDTO[] = [];
  usuarioActual: UsuarioRolesDTO | null = null;

  usuarioIdBuscado: number | null = null;
  usuarioConsultado: UsuarioRolesDTO | null = null;
  rolSeleccionadoId: number | null = null;

  cargando = false;
  mensajeError = '';
  mensajeExito = '';

  ngOnInit(): void {
    this.rolesService.listarCatalogo().subscribe({
      next: (roles) => (this.catalogo = roles),
      error: (err) => (this.mensajeError = this.extraerMensaje(err))
    });

    this.rolesService.misPermisos().subscribe({
      next: (dto) => (this.usuarioActual = dto),
      error: () => {}
    });
  }

  buscarUsuario(): void {
    if (!this.usuarioIdBuscado) return;
    this.limpiarMensajes();
    this.cargando = true;
    this.rolesService.permisosDeUsuario(this.usuarioIdBuscado).subscribe({
      next: (dto) => { this.usuarioConsultado = dto; this.cargando = false; },
      error: (err) => { this.usuarioConsultado = null; this.mensajeError = this.extraerMensaje(err); this.cargando = false; }
    });
  }

  asignarRol(): void {
    if (!this.usuarioIdBuscado || !this.rolSeleccionadoId) return;
    this.limpiarMensajes();
    this.cargando = true;
    this.rolesService.asignarRol(this.usuarioIdBuscado, this.rolSeleccionadoId).subscribe({
      next: (dto) => { this.usuarioConsultado = dto; this.mensajeExito = 'Rol asignado correctamente.'; this.cargando = false; },
      error: (err) => { this.mensajeError = this.extraerMensaje(err); this.cargando = false; }
    });
  }

  revocarRol(rolNombre: string): void {
    if (!this.usuarioIdBuscado) return;
    const rol = this.catalogo.find((r) => r.nombreRol === rolNombre);
    if (!rol) return;
    this.limpiarMensajes();
    this.cargando = true;
    this.rolesService.revocarRol(this.usuarioIdBuscado, rol.id).subscribe({
      next: (dto) => { this.usuarioConsultado = dto; this.mensajeExito = 'Rol revocado correctamente.'; this.cargando = false; },
      error: (err) => { this.mensajeError = this.extraerMensaje(err); this.cargando = false; }
    });
  }

  private limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }

  private extraerMensaje(err: HttpErrorResponse): string {
    const body = err.error as ApiErrorBody | undefined;
    if (body?.message) return body.message;
    if (err.status === 401) return 'Su sesión expiró. Inicie sesión nuevamente.';
    return 'Ocurrió un error inesperado.';
  }
}
