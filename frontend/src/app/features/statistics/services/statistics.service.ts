import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

export interface OpcionEstadistica { id: number; nombre: string; }
export interface CatalogoEstadisticas { ligas: OpcionEstadistica[]; temporadas: OpcionEstadistica[]; }
export interface PaginaOpciones { items: OpcionEstadistica[]; pagina: number; tamano: number; total: number; }
export interface FiltrosEstadisticas { jugadorId?: number; equipoId?: number; juegoId?: number; temporadaId?: number; ligaId?: number; desde?: string; hasta?: string; }
export interface ParticipacionHistorica { torneoId: number; torneoNombre: string; fecha: string; temporadaId: number|null; temporadaNombre: string|null; ligaId: number|null; ligaNombre: string|null; equipoId: number; equipoNombre: string; partidas: number; victorias: number; derrotas: number; porcentajeVictorias: number; }
export interface ResumenJuego { juegoId: number; juegoNombre: string; partidas: number; victorias: number; derrotas: number; porcentajeVictorias: number; torneos: number; lineaTiempo: ParticipacionHistorica[]; }
export interface EstadisticasHistoricas { tipoSujeto: 'JUGADOR'|'EQUIPO'; sujetoId: number; sujetoNombre: string; juegos: ResumenJuego[]; resultadosNoDefinitivos: number; }

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly api = inject(ApiService);
  catalogo(): Observable<CatalogoEstadisticas> { return this.api.get<CatalogoEstadisticas>('/statistics/catalog'); }
  buscarSujetos(tipo: 'JUGADOR'|'EQUIPO', texto: string, juegoId?: number): Observable<PaginaOpciones> {
    const juego = juegoId ? `&juegoId=${juegoId}` : '';
    return this.api.get<PaginaOpciones>(`/statistics/subjects?tipo=${tipo}&q=${encodeURIComponent(texto)}&tamano=5${juego}`);
  }
  consultar(filtros: FiltrosEstadisticas): Observable<EstadisticasHistoricas> {
    const query = Object.entries(filtros).filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&');
    return this.api.get<EstadisticasHistoricas>(`/statistics?${query}`);
  }
}
