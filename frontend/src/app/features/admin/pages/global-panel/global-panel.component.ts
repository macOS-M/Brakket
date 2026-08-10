import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminPanelService } from '../../services/admin-panel.service';
import { LogAuditoriaEntry, PanelGlobal } from '../../../../models/admin-panel.model';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../../shared/components/stat-card/stat-card.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

/**
 * Panel global de administración (RF-49, EPIC-14): tablero de supervisión de
 * toda la plataforma — conteos por módulo y actividad de auditoría reciente.
 * La gestión de roles (RF-19) vive en /admin/roles.
 */
@Component({
  selector: 'app-global-panel',
  standalone: true,
  imports: [DatePipe, RouterLink, PageHeaderComponent, StatCardComponent, EmptyStateComponent],
  templateUrl: './global-panel.component.html',
  styleUrl: './global-panel.component.scss'
})
export class GlobalPanelComponent {
  private readonly adminPanel = inject(AdminPanelService);

  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly panel = signal<PanelGlobal | null>(null);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.adminPanel.panel().subscribe({
      next: (data) => {
        this.panel.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(this.mensajeError(err));
        this.cargando.set(false);
      }
    });
  }

  /** Etiqueta del actor de una entrada de auditoría (nombre o "Sistema"). */
  actor(entrada: LogAuditoriaEntry): string {
    return entrada.actorNombre ?? 'Sistema';
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 403) return 'Necesitás rol ADMIN para ver el panel global.';
    if (err.status === 401) return 'Tu sesión expiró. Iniciá sesión nuevamente.';
    return err.error?.message ?? 'No se pudo cargar el panel de administración.';
  }
}
