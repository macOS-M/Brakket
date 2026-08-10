import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  AgrupacionMetricas,
  SeriesTransmision,
  TransmisionAnalizable,
} from '../../../models/analitica.model';

export interface FiltrosSeries {
  transmisionId: number;
  desde?: string | null;
  hasta?: string | null;
  agrupacion?: AgrupacionMetricas;
}

/** RF-37: panel analítico de transmisiones. */
@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly api = inject(ApiService);

  /** Transmisiones que el usuario puede consultar, ya filtradas por rol en el backend. */
  transmisiones(): Observable<TransmisionAnalizable[]> {
    return this.api.get<TransmisionAnalizable[]>('/analytics/transmisiones');
  }

  series(filtros: FiltrosSeries): Observable<SeriesTransmision> {
    const params = new URLSearchParams();
    if (filtros.desde) params.set('desde', filtros.desde);
    if (filtros.hasta) params.set('hasta', filtros.hasta);
    if (filtros.agrupacion) params.set('agrupacion', filtros.agrupacion);
    const query = params.toString();
    return this.api.get<SeriesTransmision>(
      `/analytics/transmisiones/${filtros.transmisionId}/series${query ? '?' + query : ''}`
    );
  }
}
