import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  CrearTorneoRequest,
  EquipoElegible,
  Torneo,
  TorneoDetalle
} from '../../../models/tournament.model';

/**
 * Servicio de datos de torneos (RF-24/RF-25, modelo abierto): torneos
 * públicos visibles para todos; el organizador ve además los suyos.
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

  crear(request: CrearTorneoRequest): Observable<Torneo> {
    return this.api.post<Torneo>('/tournaments', request);
  }

  /** Equipos del capitán autenticado elegibles para este torneo. */
  equiposElegibles(id: number): Observable<EquipoElegible[]> {
    return this.api.get<EquipoElegible[]>(`/tournaments/${id}/equipos-elegibles`);
  }

  inscribir(id: number, equipoId: number): Observable<TorneoDetalle> {
    return this.api.post<TorneoDetalle>(`/tournaments/${id}/inscripciones`, { equipoId });
  }

  eliminar(id: number): Observable<void> {
    return this.api.delete<void>(`/tournaments/${id}`);
  }
}
