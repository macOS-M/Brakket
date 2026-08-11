import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  AnalizarChatRequest,
  SentimientoResultado,
  SerieSentimiento
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
}
