import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { ApiService } from '../../../core/services/api.service';
import { CrearEspacioPublicitarioRequest, EspacioPublicitario } from '../../../models/espacio-publicitario.model';

@Injectable({ providedIn: 'root' })
export class EspaciosPublicitariosService {
  private readonly api = inject(ApiService);

  listarPorPatrocinio(patrocinioId: number): Observable<EspacioPublicitario[]> {
    return this.api.get<EspacioPublicitario[]>(`/espacios?patrocinioId=${patrocinioId}`);
  }

  crear(request: CrearEspacioPublicitarioRequest): Observable<EspacioPublicitario> {
    return this.api.post<EspacioPublicitario>('/espacios', request);
  }

  // <app-ad-slot>: espacio vigente para una ubicacion + alcance. El backend
  // devuelve 204 sin cuerpo si no hay ninguno; se normaliza a null en vez de
  // dejar que el componente adivine como interpreta HttpClient un 204.
  buscarVigente(params: {
    ubicacion: string;
    ligaId?: number;
    temporadaId?: number;
    torneoId?: number;
  }): Observable<EspacioPublicitario | null> {
    const query = new URLSearchParams();
    query.set('ubicacion', params.ubicacion);
    if (params.ligaId != null) query.set('ligaId', String(params.ligaId));
    if (params.temporadaId != null) query.set('temporadaId', String(params.temporadaId));
    if (params.torneoId != null) query.set('torneoId', String(params.torneoId));

    return this.api.get<EspacioPublicitario>(`/espacios/vigente?${query.toString()}`).pipe(
      catchError(() => of(null))
    );
  }
}
