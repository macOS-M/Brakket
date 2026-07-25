import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { CanalTwitch, MetricasTransmision, TransmisionTwitch } from '../../../models/twitch.model';

/**
 * Servicio de datos de la feature "twitch".
 * Pendiente EPIC-09.
 */
@Injectable({ providedIn: 'root' })
export class TwitchService {
  private readonly api = inject(ApiService);

  obtener(): Observable<CanalTwitch> {
    return this.api.get<CanalTwitch>('/twitch');
  }

  configurar(canal: string): Observable<CanalTwitch> {
    return this.api.post<CanalTwitch>('/twitch/canal', { canal });
  }

  validar(): Observable<CanalTwitch> {
    return this.api.post<CanalTwitch>('/twitch/validar', {});
  }

  asociar(torneoId: number | null, partidaId: number | null): Observable<TransmisionTwitch> {
    return this.api.post<TransmisionTwitch>('/twitch/transmisiones', { torneoId, partidaId });
  }

  metricas(transmisionId: number): Observable<MetricasTransmision> {
    return this.api.get<MetricasTransmision>(`/twitch/transmisiones/${transmisionId}/metricas`);
  }
}
