import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

/**
 * Servicio de datos de la feature "disputes".
 * Pendiente EPIC-08.
 */
@Injectable({ providedIn: 'root' })
export class DisputesService {
  private readonly api = inject(ApiService);

  list<T>(): Observable<T> {
    return this.api.get<T>('/disputes');
  }
}
