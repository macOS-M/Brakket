import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

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
}
