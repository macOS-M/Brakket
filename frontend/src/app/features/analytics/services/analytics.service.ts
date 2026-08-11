import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  AnalizarChatRequest,
  FiltrosTermometro,
  SentimientoResultado,
  SerieSentimiento,
  Termometro,
  TransmisionAnalizada
} from '../../../models/sentiment.model';

/**
 * Servicio de datos de la feature "analytics" (RF-39, EPIC-10).
 * Consume la API de análisis de sentimiento del chat.
 */
@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly api = inject(ApiService);

  /** Analiza un lote de mensajes de chat de la transmisión (acción de ADMIN). */
  analizar(transmisionId: number, body: AnalizarChatRequest): Observable<SentimientoResultado> {
    return this.api.post<SentimientoResultado>(
      `/analytics/transmisiones/${transmisionId}/sentimiento`,
      body
    );
  }

  /** Serie de sentimiento de la transmisión (para el termómetro de RF-40). */
  serie(transmisionId: number): Observable<SerieSentimiento> {
    return this.api.get<SerieSentimiento>(`/analytics/transmisiones/${transmisionId}/sentimiento`);
  }

  /** Transmisiones que ya tienen análisis, para el selector del termómetro (RF-40). */
  transmisionesAnalizadas(): Observable<TransmisionAnalizada[]> {
    return this.api.get<TransmisionAnalizada[]>('/analytics/transmisiones');
  }

  /** Termómetro de sentimiento de la transmisión, acotado al período (RF-40). */
  termometro(transmisionId: number, filtros: FiltrosTermometro = {}): Observable<Termometro> {
    const params = new URLSearchParams();
    if (filtros.desde) params.set('desde', filtros.desde);
    if (filtros.hasta) params.set('hasta', filtros.hasta);
    if (filtros.intervaloMinutos) params.set('intervaloMinutos', String(filtros.intervaloMinutos));
    const query = params.toString();
    return this.api.get<Termometro>(
      `/analytics/transmisiones/${transmisionId}/sentimiento/termometro${query ? '?' + query : ''}`
    );
  }
}
