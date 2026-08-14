import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { ApiService } from '../../../core/services/api.service';
import { CrearEspacioPublicitarioRequest, EditarEspacioPublicitarioRequest, EspacioPublicitario } from '../../../models/espacio-publicitario.model';

@Injectable({ providedIn: 'root' })
export class EspaciosPublicitariosService {
  private readonly api = inject(ApiService);

  listarPorPatrocinio(patrocinioId: number): Observable<EspacioPublicitario[]> {
    return this.api.get<EspacioPublicitario[]>(`/espacios?patrocinioId=${patrocinioId}`);
  }

  crear(request: CrearEspacioPublicitarioRequest): Observable<EspacioPublicitario> {
    return this.api.post<EspacioPublicitario>('/espacios', request);
  }

  editar(id: number, request: EditarEspacioPublicitarioRequest): Observable<EspacioPublicitario> {
    return this.api.put<EspacioPublicitario>(`/espacios/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.api.delete<void>(`/espacios/${id}`);
  }

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
