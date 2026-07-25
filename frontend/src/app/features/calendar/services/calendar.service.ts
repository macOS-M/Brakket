import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { CalendarioEvento } from '../../../models/calendario-evento.model';

export interface FiltrosCalendario {
  desde?: string | null;
  hasta?: string | null;
  juegoId?: number | null;
  ligaId?: number | null;
  estado?: string | null;
}

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private readonly api = inject(ApiService);

  consultar(filtros: FiltrosCalendario): Observable<CalendarioEvento[]> {
    const params = new URLSearchParams();
    if (filtros.desde) params.set('desde', filtros.desde);
    if (filtros.hasta) params.set('hasta', filtros.hasta);
    if (filtros.juegoId) params.set('juegoId', String(filtros.juegoId));
    if (filtros.ligaId) params.set('ligaId', String(filtros.ligaId));
    if (filtros.estado) params.set('estado', filtros.estado);
    const query = params.toString();
    return this.api.get<CalendarioEvento[]>(`/calendar${query ? '?' + query : ''}`);
  }
}
