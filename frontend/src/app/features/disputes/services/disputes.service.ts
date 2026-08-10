import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { AdjuntarEvidenciaRequest, EvidenciaResponse } from '../../../models/evidencia.model';
import {
  ApelacionResponse,
  ApelarRequest,
  DisputaResponse,
  ResolverApelacionRequest,
  ResolverDisputaRequest
} from '../../../models/disputa.model';

/** Servicio de datos de disputas (RF-30/31/32/33). */
@Injectable({ providedIn: 'root' })
export class DisputesService {
  private readonly api = inject(ApiService);

  /** RF-31: adjuntar evidencia a una disputa abierta. */
  adjuntarEvidencia(disputaId: number, request: AdjuntarEvidenciaRequest): Observable<EvidenciaResponse> {
    return this.api.post<EvidenciaResponse>(`/disputas/${disputaId}/evidencias`, request);
  }

  listarEvidencias(disputaId: number): Observable<EvidenciaResponse[]> {
    return this.api.get<EvidenciaResponse[]>(`/disputas/${disputaId}/evidencias`);
  }

  /** RF-32: árbitro/comisionado/admin resuelven la disputa. */
  resolverDisputa(disputaId: number, request: ResolverDisputaRequest): Observable<DisputaResponse> {
    return this.api.post<DisputaResponse>(`/disputas/${disputaId}/resolucion`, request);
  }

  /** RF-32: apelar una disputa ya resuelta, dentro del plazo. */
  apelar(disputaId: number, request: ApelarRequest): Observable<ApelacionResponse> {
    return this.api.post<ApelacionResponse>(`/disputas/${disputaId}/apelaciones`, request);
  }

  listarApelaciones(disputaId: number): Observable<ApelacionResponse[]> {
    return this.api.get<ApelacionResponse[]>(`/disputas/${disputaId}/apelaciones`);
  }

  /** Solo el comisionado de la liga (o admin) resuelve la apelación. */
  resolverApelacion(apelacionId: number, request: ResolverApelacionRequest): Observable<ApelacionResponse> {
    return this.api.post<ApelacionResponse>(`/apelaciones/${apelacionId}/resolucion`, request);
  }
}
