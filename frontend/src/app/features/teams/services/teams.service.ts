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
import { AsignarRolRequest, ExpulsarIntegranteRequest, MiembroEquipo } from '../../../models/miembro-equipo.model';
import { Invitacion, InvitarJugadorRequest, ResponderInvitacionRequest } from '../../../models/invitacion.model';
import { EquipoResumenPublico, PerfilEquipoPublico } from '../../../models/perfil-equipo-publico.model';
import { JugadorDisponible } from '../../../models/jugador-disponible.model';
import { SolicitudUnion } from '../../../models/solicitud-union.model';

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

  /** RF-10: expulsa a un integrante de la plantilla (baja lógica con causa). */
  expulsar(equipoId: number, usuarioId: number, request: ExpulsarIntegranteRequest): Observable<MiembroEquipo> {
    return this.api.patch<MiembroEquipo>(`/teams/${equipoId}/miembros/${usuarioId}/expulsar`, request);
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

  /** Equipos donde el usuario autenticado es miembro activo. */
  misEquipos(): Observable<EquipoBusqueda[]> {
    return this.api.get<EquipoBusqueda[]>('/teams/mios');
  }

  /** Un jugador pide unirse a un equipo ajeno; responde el capitán. */
  solicitarUnion(equipoId: number, mensaje: string | null): Observable<SolicitudUnion> {
    return this.api.post<SolicitudUnion>(`/teams/${equipoId}/solicitudes`, { mensaje });
  }

  /** Solicitudes pendientes del equipo (solo su capitán). */
  solicitudesPendientes(equipoId: number): Observable<SolicitudUnion[]> {
    return this.api.get<SolicitudUnion[]>(`/teams/${equipoId}/solicitudes`);
  }

  responderSolicitud(solicitudId: number, aceptar: boolean): Observable<SolicitudUnion> {
    return this.api.patch<SolicitudUnion>(`/solicitudes/${solicitudId}/responder`, { aceptar });
  }

  disolver(equipoId: number, request: DisolverEquipoRequest): Observable<Equipo> {
    return this.api.patch<Equipo>(`/teams/${equipoId}/disolver`, request);
  }

  buscarJugadores(
    equipoId: number,
    texto: string,
    juegoId: number | null,
    soloDisponibles: boolean,
    page = 0,
    size = 12
  ): Observable<Pagina<JugadorDisponible>> {
    const params = new URLSearchParams();
    if (texto) {
      params.set('texto', texto);
    }
    if (juegoId !== null) {
      params.set('juegoId', String(juegoId));
    }
    params.set('soloDisponibles', String(soloDisponibles));
    params.set('page', String(page));
    params.set('size', String(size));
    return this.api.get<Pagina<JugadorDisponible>>(
      `/teams/${equipoId}/jugadores-disponibles?${params.toString()}`
    );
  }
}
