import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { CrearPatrocinioRequest, Patrocinio } from '../../../models/patrocinio.model';

@Injectable({ providedIn: 'root' })
export class PatrociniosService {
  private readonly api = inject(ApiService);

  listarPorTorneo(torneoId: number): Observable<Patrocinio[]> {
    return this.api.get<Patrocinio[]>(`/patrocinios?torneoId=${torneoId}`);
  }

  listarPorLiga(ligaId: number): Observable<Patrocinio[]> {
    return this.api.get<Patrocinio[]>(`/patrocinios?ligaId=${ligaId}`);
  }

  listarPorTemporada(temporadaId: number): Observable<Patrocinio[]> {
    return this.api.get<Patrocinio[]>(`/patrocinios?temporadaId=${temporadaId}`);
  }

  listarTodos(): Observable<Patrocinio[]> {
    return this.api.get<Patrocinio[]>('/patrocinios');
  }

  obtener(id: number): Observable<Patrocinio> {
    return this.api.get<Patrocinio>(`/patrocinios/${id}`);
  }

  crear(request: CrearPatrocinioRequest): Observable<Patrocinio> {
    return this.api.post<Patrocinio>('/patrocinios', request);
  }

  // Asume DELETE /api/patrocinios/{id} — patrón REST estándar del resto del
  // proyecto (mismo prefijo que crear/obtener). Falta confirmar contra el
  // controller real por si la ruta o el verbo difieren.
  eliminar(id: number): Observable<void> {
    return this.api.delete<void>(`/patrocinios/${id}`);
  }
}
