import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { AdjuntarEvidenciaRequest, EvidenciaResponse } from '../../../models/evidencia.model';
import { DisputaResponse } from '../../../models/disputa.model';

/** Servicio de datos de disputas (RF-30/31/32/33). */
@Injectable({ providedIn: 'root' })
export class DisputesService {
  private readonly api = inject(ApiService);

  /**  adjuntar evidencia a una disputa abierta. */
  adjuntarEvidencia(disputaId: number, request: AdjuntarEvidenciaRequest): Observable<EvidenciaResponse> {
    return this.api.post<EvidenciaResponse>(`/disputas/${disputaId}/evidencias`, request);
  }

  listarEvidencias(disputaId: number): Observable<EvidenciaResponse[]> {
    return this.api.get<EvidenciaResponse[]>(`/disputas/${disputaId}/evidencias`);
  }
}
