import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { MetricasPatrocinio, PanelComercial } from '../../../models/panel-comercial.model';

@Injectable({ providedIn: 'root' })
export class PanelComercialService {
  private readonly api = inject(ApiService);

  obtenerResumen(): Observable<PanelComercial> {
    return this.api.get<PanelComercial>('/sponsors/me/panel');
  }

  obtenerMetricas(patrocinioId: number): Observable<MetricasPatrocinio> {
    return this.api.get<MetricasPatrocinio>(`/sponsors/me/panel/metricas?patrocinioId=${patrocinioId}`);
  }
}
