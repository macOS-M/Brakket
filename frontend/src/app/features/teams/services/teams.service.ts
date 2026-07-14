import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { AsignarRolRequest, MiembroEquipo } from '../../../models/miembro-equipo.model';
import { ActualizarEquipoRequest, CrearEquipoRequest, Equipo } from '../../../models/equipo.model';
import { PerfilEquipoPublico } from '../../../models/perfil-equipo-publico.model';

@Injectable({ providedIn: 'root' })
export class TeamsService {
  private readonly api = inject(ApiService);

  crear(request: CrearEquipoRequest): Observable<Equipo> {
    return this.api.post<Equipo>('/teams', request);
  }

  listarPublicos(criterio = ''): Observable<PerfilEquipoPublico[]> {
    return this.api.get<PerfilEquipoPublico[]>(`/public/teams?criterio=${encodeURIComponent(criterio)}`);
  }

  consultarPerfilPublico(equipoId: number, juegoId?: number): Observable<PerfilEquipoPublico> {
    const filtro = juegoId ? `?juegoId=${juegoId}` : '';
    return this.api.get<PerfilEquipoPublico>(`/public/teams/${equipoId}${filtro}`);
  }

  actualizar(equipoId: number, request: ActualizarEquipoRequest): Observable<Equipo> {
    return this.api.put<Equipo>(`/teams/${equipoId}`, request);
  }

  listMiembros(equipoId: number): Observable<MiembroEquipo[]> {
    return this.api.get<MiembroEquipo[]>(`/teams/${equipoId}/miembros`);
  }

  cambiarRol(equipoId: number, usuarioId: number, request: AsignarRolRequest): Observable<MiembroEquipo> {
    return this.api.patch<MiembroEquipo>(`/teams/${equipoId}/miembros/${usuarioId}/rol`, request);
  }
}
