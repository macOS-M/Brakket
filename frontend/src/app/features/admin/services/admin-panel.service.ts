import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { LogAuditoriaEntry, PanelGlobal } from '../../../models/admin-panel.model';

/**
 * Servicio de datos del panel global de administración (RF-49, EPIC-14).
 */
@Injectable({ providedIn: 'root' })
export class AdminPanelService {
  private readonly api = inject(ApiService);

  /** Tablero: conteos globales + actividad de auditoría reciente. */
  panel(): Observable<PanelGlobal> {
    return this.api.get<PanelGlobal>('/admin/panel');
  }

  /** Listado de auditoría reciente (por defecto 50 entradas). */
  auditoria(limite = 50): Observable<LogAuditoriaEntry[]> {
    return this.api.get<LogAuditoriaEntry[]>(`/admin/auditoria?limite=${limite}`);
  }
}
