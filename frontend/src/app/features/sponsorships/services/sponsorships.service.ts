import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  CrearPatrocinadorRequest,
  EditarPatrocinadorRequest,
  Patrocinador
} from '../../../models/patrocinador.model';

@Injectable({ providedIn: 'root' })
export class SponsorshipsService {
  private readonly api = inject(ApiService);

  listar(): Observable<Patrocinador[]> {
    return this.api.get<Patrocinador[]>('/sponsors');
  }

  obtener(id: number): Observable<Patrocinador> {
    return this.api.get<Patrocinador>(`/sponsors/${id}`);
  }

  crear(request: CrearPatrocinadorRequest): Observable<Patrocinador> {
    return this.api.post<Patrocinador>('/sponsors', request);
  }

  editar(id: number, request: EditarPatrocinadorRequest): Observable<Patrocinador> {
    return this.api.put<Patrocinador>(`/sponsors/${id}`, request);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Patrocinador> {
    return this.api.patch<Patrocinador>(`/sponsors/${id}/estado`, { activo });
  }
}
