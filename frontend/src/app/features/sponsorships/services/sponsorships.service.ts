import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';

/**
 * Servicio de datos de la feature "sponsorships".
 * Pendiente EPIC-11.
 */
@Injectable({ providedIn: 'root' })
export class SponsorshipsService {
  private readonly api = inject(ApiService);

  list<T>(): Observable<T> {
    return this.api.get<T>('/sponsorships');
  }
}
