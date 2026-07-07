import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

/**
 * Servicio de datos de la feature "games".
 * Pendiente EPIC-05.
 */
@Injectable({ providedIn: 'root' })
export class GamesService {
  private readonly api = inject(ApiService);

  list<T>(): Observable<T> {
    return this.api.get<T>('/games');
  }
}
