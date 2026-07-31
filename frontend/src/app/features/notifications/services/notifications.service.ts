import { Injectable, inject } from '@angular/core';
import { Observable, Subject, map, tap } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { Notificacion } from '../../../models/notificacion.model';

/**
 * Servicio de datos de la feature "notifications".
 * Pendiente EPIC-12.
 */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly api = inject(ApiService);
  private readonly cambios = new Subject<void>();
  readonly cambios$ = this.cambios.asObservable();

  list(limit?: number): Observable<Notificacion[]> {
    const query = limit === undefined ? '' : `?limit=${limit}`;
    return this.api.get<Notificacion[]>(`/notifications${query}`);
  }

  unreadCount(): Observable<number> {
    return this.api.get<{ count: number }>('/notifications/unread-count').pipe(map(({ count }) => count));
  }

  markRead(id: number): Observable<Notificacion> {
    return this.api.patch<Notificacion>(`/notifications/${id}/read`, {}).pipe(
      tap(() => this.cambios.next())
    );
  }

  markAllRead(): Observable<void> {
    return this.api.patch<void>('/notifications/read-all', {}).pipe(
      tap(() => this.cambios.next())
    );
  }

  remove(id: number): Observable<void> {
    return this.api.delete<void>(`/notifications/${id}`).pipe(
      tap(() => this.cambios.next())
    );
  }

  /**
   * Resuelve la pantalla funcional asociada al evento. Algunas entidades,
   * como disputas y transmisiones, todavía no tienen una ruta de detalle;
   * en esos casos se abre su listado contextual.
   */
  destination(notification: Notificacion): (string | number)[] {
    const entity = notification.entidad?.replace(/[^a-z0-9]/gi, '').toLowerCase();
    const type = notification.tipo;

    if (entity === 'torneo' || entity === 'tournament') {
      return ['/tournaments', notification.entidadId];
    }
    if (entity === 'equipo' || entity === 'team') {
      return ['/team-profile', notification.entidadId];
    }
    if (entity === 'miembroequipo') {
      return ['/teams'];
    }
    if (entity === 'disputa' || type === 'DISPUTA') {
      return ['/disputes'];
    }
    if (entity === 'transmision' || type === 'TRANSMISION') {
      return ['/transmisiones'];
    }
    if (
      entity === 'invitacionequipo' ||
      type.startsWith('INVITACION') ||
      type.startsWith('SOLICITUD_')
    ) {
      return ['/teams/invitaciones'];
    }
    if (entity === 'solicitudtransferencia' || type.startsWith('TRANSFERENCIA')) {
      return ['/transfers'];
    }
    if (entity === 'partida' || type === 'RESULTADO' || type === 'CAMBIO_TORNEO') {
      return ['/tournaments'];
    }
    if (type === 'ADMINISTRATIVA' || entity === 'accionadministrativa') {
      return ['/inicio'];
    }
    return ['/notifications'];
  }
}
