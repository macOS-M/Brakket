import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CatalogoCompetitivo,
  PerfilCompetitivoRequest,
  PerfilCompetitivoResponse
} from '../../models/perfil-competitivo.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class CompetitiveProfileService {
  private readonly api = inject(ApiService);

  obtenerPorJuego(juegoId: number): Observable<PerfilCompetitivoResponse> {
    return this.api.get<PerfilCompetitivoResponse>(`/competitive-profiles/game/${juegoId}`);
  }

  crear(request: PerfilCompetitivoRequest): Observable<PerfilCompetitivoResponse> {
    return this.api.post<PerfilCompetitivoResponse>('/competitive-profiles', request);
  }

  actualizar(id: number, request: PerfilCompetitivoRequest): Observable<PerfilCompetitivoResponse> {
    return this.api.put<PerfilCompetitivoResponse>(`/competitive-profiles/${id}`, request);
  }

  listarFormatos(): Observable<CatalogoCompetitivo[]> {
    return this.api.get<CatalogoCompetitivo[]>('/competitive-catalogs/formats');
  }

  listarEstadisticas(): Observable<CatalogoCompetitivo[]> {
    return this.api.get<CatalogoCompetitivo[]>('/competitive-catalogs/statistics');
  }
}
