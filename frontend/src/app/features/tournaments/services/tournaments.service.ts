import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

/**
 * Servicio de datos de la feature "tournaments".
 * Pendiente EPIC-07.
 */
@Injectable({ providedIn: 'root' })
export class TournamentsService {
  private readonly api = inject(ApiService);

  list<T>(): Observable<T> {
    return this.api.get<T>('/tournaments');
  }
}
