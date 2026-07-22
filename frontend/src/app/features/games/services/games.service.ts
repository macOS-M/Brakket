import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { Juego, JuegoExterno, JuegoRequest } from '../../../models/juego.model';

/**
 * Servicio de datos de la feature "games" (RF-20).
 */
@Injectable({ providedIn: 'root' })
export class GamesService {
  private readonly api = inject(ApiService);

  listActivos(): Observable<Juego[]> {
    return this.api.get<Juego[]>('/games');
  }

  /** Busca en el catálogo externo (RAWG) vía el proxy del backend. */
  buscarExterno(consulta: string): Observable<JuegoExterno[]> {
    return this.api.get<JuegoExterno[]>(
      `/games/buscar-externo?q=${encodeURIComponent(consulta)}`
    );
  }

  obtenerPorId(id: number): Observable<Juego> {
    return this.api.get<Juego>(`/games/${id}`);
  }

  crear(request: JuegoRequest): Observable<Juego> {
    return this.api.post<Juego>('/games', request);
  }

  editar(id: number, request: JuegoRequest): Observable<Juego> {
    return this.api.put<Juego>(`/games/${id}`, request);
  }

  desactivar(id: number): Observable<void> {
    return this.api.patch<void>(`/games/${id}/desactivar`, {});
  }
}
