import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  CrearTorneoRequest,
  EquipoElegible,
  Partida,
  Torneo,
  TorneoDetalle
} from '../../../models/tournament.model';
import { DisputaResponse, ImpugnarResultadoRequest } from '../../../models/disputa.model';

/**
 * Servicio de datos de torneos (RF-24/RF-25, modelo abierto): torneos
 * públicos visibles para todos; el organizador ve además los suyos.
 * Incluye el torneo en vivo (RF-26/27/29): bracket y resultados.
 */
@Injectable({ providedIn: 'root' })
export class TournamentsService {
  private readonly api = inject(ApiService);

  /** Torneos visibles, opcionalmente de un solo juego. */
  listar(juegoId?: number): Observable<Torneo[]> {
    const filtro = juegoId ? `?juegoId=${juegoId}` : '';
    return this.api.get<Torneo[]>(`/tournaments${filtro}`);
  }

  obtener(id: number): Observable<TorneoDetalle> {
    return this.api.get<TorneoDetalle>(`/tournaments/${id}`);
  }

  /** " competencias": los que organizo + donde compite mi equipo. */
  misCompetencias(): Observable<Torneo[]> {
    return this.api.get<Torneo[]>('/tournaments/mios');
  }

  crear(request: CrearTorneoRequest): Observable<Torneo> {
    return this.api.post<Torneo>('/tournaments', request);
  }

  /** Equipos del capitán autenticado elegibles para este torneo. */
  equiposElegibles(id: number): Observable<EquipoElegible[]> {
    return this.api.get<EquipoElegible[]>(`/tournaments/${id}/equipos-elegibles`);
  }

  inscribir(id: number, equipoId: number, usuarioEnJuego: string): Observable<TorneoDetalle> {
    return this.api.post<TorneoDetalle>(
      `/tournaments/${id}/inscripciones`, { equipoId, usuarioEnJuego });
  }

  eliminar(id: number): Observable<void> {
    return this.api.delete<void>(`/tournaments/${id}`);
  }

  // ---------- torneo en vivo ----------

  /** Cierra inscripciones, genera el bracket y pone el torneo en curso. */
  iniciar(id: number): Observable<Partida[]> {
    return this.api.post<Partida[]>(`/tournaments/${id}/iniciar`, {});
  }

  bracket(id: number): Observable<Partida[]> {
    return this.api.get<Partida[]>(`/tournaments/${id}/bracket`);
  }

  /** Un capitán de la partida reporta el marcador. */
  reportar(partidaId: number, marcadorA: number, marcadorB: number): Observable<Partida> {
    return this.api.post<Partida>(
      `/tournaments/partidas/${partidaId}/reporte`, { marcadorA, marcadorB });
  }

  /** El capitán rival confirma; el ganador avanza en la llave. */
  confirmar(partidaId: number): Observable<Partida> {
    return this.api.post<Partida>(`/tournaments/partidas/${partidaId}/confirmacion`, {});
  }

  /** El capitán rival rechaza el reporte: queda en disputa. */
  rechazar(partidaId: number): Observable<Partida> {
    return this.api.post<Partida>(`/tournaments/partidas/${partidaId}/rechazo`, {});
  }

  /** El organizador (o un ADMIN) fija el resultado final. */
  resolver(partidaId: number, marcadorA: number, marcadorB: number): Observable<Partida> {
    return this.api.post<Partida>(
      `/tournaments/partidas/${partidaId}/resolucion`, { marcadorA, marcadorB });
  }

  /** Impugnar un resultado ya finalizado, dentro del plazo (RF-30). */
  impugnar(partidaId: number, request: ImpugnarResultadoRequest): Observable<DisputaResponse> {
    return this.api.post<DisputaResponse>(`/tournaments/partidas/${partidaId}/disputas`, request);
  }
}
