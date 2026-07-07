import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

/**
 * Servicio de datos de la feature "statistics".
 * Pendiente EPIC-13.
 */
@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly api = inject(ApiService);

  list<T>(): Observable<T> {
    return this.api.get<T>('/statistics');
  }
}
