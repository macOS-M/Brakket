import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { FiltrosReporte, ReporteResponse } from '../../../models/reporte.model';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly api = inject(ApiService);

  generar(filtros: FiltrosReporte): Observable<ReporteResponse> {
    return this.api.get<ReporteResponse>(`/reports?${this.construirQuery(filtros)}`);
  }

  generarPdf(filtros: FiltrosReporte): Observable<Blob> {
    return this.api.getBlob(`/reports/pdf?${this.construirQuery(filtros)}`);
  }

  private construirQuery(filtros: FiltrosReporte): string {
    const params = new URLSearchParams();
    params.set('tipo', filtros.tipo);
    if (filtros.torneoId != null) params.set('torneoId', String(filtros.torneoId));
    if (filtros.patrocinadorId != null) params.set('patrocinadorId', String(filtros.patrocinadorId));
    if (filtros.desde) params.set('desde', filtros.desde);
    if (filtros.hasta) params.set('hasta', filtros.hasta);
    return params.toString();
  }
}
