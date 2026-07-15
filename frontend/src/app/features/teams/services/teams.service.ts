import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { AsignarRolRequest, MiembroEquipo } from '../../../models/miembro-equipo.model';
import { CrearEquipoRequest, Equipo } from '../../../models/equipo.model';
import { Invitacion, InvitarJugadorRequest, ResponderInvitacionRequest } from '../../../models/invitacion.model';

@Injectable({ providedIn: 'root' })
export class TeamsService {
  private readonly api = inject(ApiService);

  crear(request: CrearEquipoRequest): Observable<Equipo> {
    return this.api.post<Equipo>('/teams', request);
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
}
