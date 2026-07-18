import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  BuscarEquiposParams,
  CrearEquipoRequest,
  DisolverEquipoRequest,
  EditarEquipoRequest,
  Equipo,
  EquipoBusqueda,
  Pagina
} from '../../../models/equipo.model';
import { AsignarRolRequest, MiembroEquipo } from '../../../models/miembro-equipo.model';
import { Invitacion, InvitarJugadorRequest, ResponderInvitacionRequest } from '../../../models/invitacion.model';
import { EquipoResumenPublico, PerfilEquipoPublico } from '../../../models/perfil-equipo-publico.model';

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

  invitar(equipoId: number, request: InvitarJugadorRequest): Observable<Invitacion> {
    return this.api.post<Invitacion>(`/teams/${equipoId}/invitaciones`, request);
  }

  misInvitacionesPendientes(): Observable<Invitacion[]> {
    return this.api.get<Invitacion[]>('/invitaciones/pendientes');
  }

  responderInvitacion(invitacionId: number, request: ResponderInvitacionRequest): Observable<Invitacion> {
    return this.api.patch<Invitacion>(`/invitaciones/${invitacionId}/responder`, request);
  }

  disolver(equipoId: number, request: DisolverEquipoRequest): Observable<Equipo> {
    return this.api.patch<Equipo>(`/teams/${equipoId}/disolver`, request);
  }
}
