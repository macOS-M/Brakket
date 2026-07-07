import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

/**
 * Servicio de datos de la feature "teams".
 * Pendiente EPIC-04.
 */
@Injectable({ providedIn: 'root' })
export class TeamsService {
  private readonly api = inject(ApiService);

  list<T>(): Observable<T> {
    return this.api.get<T>('/teams');
  }
}
