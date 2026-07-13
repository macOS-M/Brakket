import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { AsignarRolRequest, MiembroEquipo } from '../../../models/miembro-equipo.model';
import {
  BuscarEquiposParams,
  CrearEquipoRequest,
  Equipo,
  EquipoBusqueda,
  Pagina
} from '../../../models/equipo.model';

@Injectable({ providedIn: 'root' })
export class TeamsService {
  private readonly api = inject(ApiService);

  crear(request: CrearEquipoRequest): Observable<Equipo> {
    return this.api.post<Equipo>('/teams', request);
  }

  /** Búsqueda de equipos con filtros y paginación (RF-05). */
  buscar(params: BuscarEquiposParams): Observable<Pagina<EquipoBusqueda>> {
    const query = new URLSearchParams();
    if (params.q) query.set('q', params.q);
    if (params.juegoId != null) query.set('juegoId', String(params.juegoId));
    if (params.disciplina) query.set('disciplina', params.disciplina);
    if (params.estado) query.set('estado', params.estado);
    if (params.page != null) query.set('page', String(params.page));
    if (params.size != null) query.set('size', String(params.size));
    const sufijo = query.toString() ? `?${query.toString()}` : '';
    return this.api.get<Pagina<EquipoBusqueda>>(`/teams/search${sufijo}`);
  }

  listMiembros(equipoId: number): Observable<MiembroEquipo[]> {
    return this.api.get<MiembroEquipo[]>(`/teams/${equipoId}/miembros`);
  }

  cambiarRol(equipoId: number, usuarioId: number, request: AsignarRolRequest): Observable<MiembroEquipo> {
    return this.api.patch<MiembroEquipo>(`/teams/${equipoId}/miembros/${usuarioId}/rol`, request);
  }
}
