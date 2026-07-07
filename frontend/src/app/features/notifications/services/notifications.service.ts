import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

/**
 * Servicio de datos de la feature "notifications".
 * Pendiente EPIC-12.
 */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly api = inject(ApiService);

  list<T>(): Observable<T> {
    return this.api.get<T>('/notifications');
  }
}
