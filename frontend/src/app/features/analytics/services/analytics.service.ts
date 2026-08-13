import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  AnalizarChatRequest,
  AsistenteRespuesta,
  ClasificacionInmediata,
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
    return this.api.get<TransmisionAnalizada[]>('/analytics/transmisiones/analizadas');
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

  /**
   * Pregunta al asistente sobre la transmisión (RF-40, solo ADMIN).
   *
   * <p>Cada consulta es independiente: el backend no guarda la conversación, así
   * que el historial que ve el usuario es solo de la vista y el modelo no lee
   * los turnos anteriores. Por eso conviene que cada pregunta se baste sola.</p>
   */
  preguntarAsistente(
    transmisionId: number,
    pregunta: string,
    filtros: FiltrosTermometro = {}
  ): Observable<AsistenteRespuesta> {
    const params = new URLSearchParams();
    if (filtros.desde) params.set('desde', filtros.desde);
    if (filtros.hasta) params.set('hasta', filtros.hasta);
    const query = params.toString();
    return this.api.post<AsistenteRespuesta>(
      `/analytics/transmisiones/${transmisionId}/asistente${query ? '?' + query : ''}`,
      { pregunta }
    );
  }

  /**
   * Clasifica el chat acumulado sin esperar al bloque (RF-39, solo ADMIN).
   *
   * <p>No lleva id: el muestreo atiende una transmisión a la vez y cierra el
   * bloque que tenga en curso. La petición espera al proveedor, así que puede
   * tardar unos segundos.</p>
   */
  clasificarSentimientoAhora(): Observable<ClasificacionInmediata> {
    return this.api.post<ClasificacionInmediata>('/analytics/muestreo/sentimiento', {});
  }
}
