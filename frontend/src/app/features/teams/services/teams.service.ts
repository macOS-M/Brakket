import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  CrearEquipoRequest,
  DisolverEquipoRequest,
  EditarEquipoRequest,
  Equipo
} from '../../../models/equipo.model';
import { AsignarRolRequest, MiembroEquipo } from '../../../models/miembro-equipo.model';
import { EquipoResumenPublico, PerfilEquipoPublico } from '../../../models/perfil-equipo-publico.model';
import { InvitarJugadorRequest, JugadorDisponible } from '../../../models/jugador-disponible.model';

@Injectable({ providedIn: 'root' })
export class TeamsService {
  private readonly api = inject(ApiService);

  crear(request: CrearEquipoRequest): Observable<Equipo> {
    return this.api.post<Equipo>('/teams', request);
  }

  listarPublicos(criterio = ''): Observable<EquipoResumenPublico[]> {
    return this.api.get<EquipoResumenPublico[]>(`/public/teams?criterio=${encodeURIComponent(criterio)}`);
  }

  consultarPerfilPublico(equipoId: number, juegoId?: number): Observable<PerfilEquipoPublico> {
    const filtro = juegoId ? `?juegoId=${juegoId}` : '';
    return this.api.get<PerfilEquipoPublico>(`/public/teams/${equipoId}${filtro}`);
  }

  obtenerPorId(equipoId: number): Observable<Equipo> {
    return this.api.get<Equipo>(`/teams/${equipoId}`);
  }

  editar(equipoId: number, request: EditarEquipoRequest): Observable<Equipo> {
    return this.api.put<Equipo>(`/teams/${equipoId}`, request);
  }

  listMiembros(equipoId: number): Observable<MiembroEquipo[]> {
    return this.api.get<MiembroEquipo[]>(`/teams/${equipoId}/miembros`);
  }

  cambiarRol(equipoId: number, usuarioId: number, request: AsignarRolRequest): Observable<MiembroEquipo> {
    return this.api.patch<MiembroEquipo>(`/teams/${equipoId}/miembros/${usuarioId}/rol`, request);
  }

  disolver(equipoId: number, request: DisolverEquipoRequest): Observable<Equipo> {
    return this.api.patch<Equipo>(`/teams/${equipoId}/disolver`, request);
  }

  buscarJugadores(equipoId: number, texto: string, juegoId: number | null,
                  soloDisponibles: boolean): Observable<JugadorDisponible[]> {
    const params = new URLSearchParams();
    if (texto) {
      params.set('texto', texto);
    }
    if (juegoId) {
      params.set('juegoId', String(juegoId));
    }
    params.set('soloDisponibles', String(soloDisponibles));
    return this.api.get<JugadorDisponible[]>(`/teams/${equipoId}/jugadores-disponibles?${params.toString()}`);
  }

  invitar(equipoId: number, request: InvitarJugadorRequest): Observable<unknown> {
    return this.api.post<unknown>(`/teams/${equipoId}/invitaciones`, request);
  }
}
