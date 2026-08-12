import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

export interface LogroProgresion { id:number; nombre:string; descripcion:string; puntos:number; origen:string; desbloqueado:boolean; fecha:string|null; }
export interface ElementoProgresion { id:number; nombre:string; descripcion:string; tipo:'TITULO'|'MARCO'|'INSIGNIA'; costo:number; activo:boolean; canjeado:boolean; aplicado:boolean; habilitado:boolean; requisito:string; }
export interface Progresion { puntos:number; logros:LogroProgresion[]; elementos:ElementoProgresion[]; }

@Injectable({ providedIn: 'root' })
export class ProgressionService {
  private readonly api = inject(ApiService);
  get(): Observable<Progresion> { return this.api.get<Progresion>('/progression'); }
  redeem(id:number): Observable<Progresion> { return this.api.post<Progresion>(`/progression/redeem/${id}`, {}); }
  apply(id:number): Observable<Progresion> { return this.api.put<Progresion>(`/progression/apply/${id}`, {}); }
  remove(id:number): Observable<Progresion> { return this.api.delete<Progresion>(`/progression/apply/${id}`); }
}
