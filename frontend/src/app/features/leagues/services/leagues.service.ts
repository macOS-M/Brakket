import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  GameOption,
  League,
  LeagueRequest,
  Season,
  SeasonRequest
} from '../../../models/league.model';

/**
 * Servicio de datos de la feature "leagues" (RF-22, EPIC-07).
 * Consume la API de ligas y temporadas a través del {@link ApiService}.
 */
@Injectable({ providedIn: 'root' })
export class LeaguesService {
  private readonly api = inject(ApiService);

  /** Todas las ligas. */
  list(): Observable<League[]> {
    return this.api.get<League[]>('/leagues');
  }

  /** Detalle de una liga. */
  getById(id: number): Observable<League> {
    return this.api.get<League>(`/leagues/${id}`);
  }

  /** Crea una liga (el comisionado es el usuario autenticado). */
  create(body: LeagueRequest): Observable<League> {
    return this.api.post<League>('/leagues', body);
  }

  /** Configura/edita una liga existente. */
  update(id: number, body: LeagueRequest): Observable<League> {
    return this.api.put<League>(`/leagues/${id}`, body);
  }

  /** Elimina una liga y sus temporadas (solo comisionado o admin). */
  delete(id: number): Observable<void> {
    return this.api.delete<void>(`/leagues/${id}`);
  }

  /** Temporadas de una liga. */
  listSeasons(ligaId: number): Observable<Season[]> {
    return this.api.get<Season[]>(`/leagues/${ligaId}/seasons`);
  }

  /** Agrega una temporada a la liga. */
  createSeason(ligaId: number, body: SeasonRequest): Observable<Season> {
    return this.api.post<Season>(`/leagues/${ligaId}/seasons`, body);
  }

  /** Juegos activos disponibles para el selector del formulario. */
  gameOptions(): Observable<GameOption[]> {
    return this.api.get<GameOption[]>('/leagues/game-options');
  }
}
