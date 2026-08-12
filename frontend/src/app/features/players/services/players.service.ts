import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { HistorialEquipoJugador } from '../../../models/historial-jugador.model';

export interface ElementoPerfil { id:number; nombre:string; descripcion:string; }
export interface PerfilPersonalizado { jugadorId:number; nombre:string; titulo:ElementoPerfil|null; insignia:ElementoPerfil|null; }

@Injectable({ providedIn: 'root' })
export class PlayersService {
  private readonly api = inject(ApiService);

  historial(jugadorId: number, juegoId: number | null, desde: string | null,
            hasta: string | null): Observable<HistorialEquipoJugador[]> {
    const params = new URLSearchParams();
    if (juegoId) {
      params.set('juegoId', String(juegoId));
    }
    if (desde) {
      params.set('desde', desde);
    }
    if (hasta) {
      params.set('hasta', hasta);
    }
    const query = params.toString();
    return this.api.get<HistorialEquipoJugador[]>(
      `/players/${jugadorId}/historial-equipos${query ? '?' + query : ''}`
    );
  }

  personalizacion(jugadorId:number):Observable<PerfilPersonalizado> {
    return this.api.get<PerfilPersonalizado>(`/public/players/${jugadorId}/customization`);
  }
}
