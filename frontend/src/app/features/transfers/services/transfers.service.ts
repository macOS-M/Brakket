import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { CrearTransferenciaRequest, Transferencia } from '../../../models/transferencia.model';

/**
 * Servicio de datos de la feature "transfers" (RF-12).
 */
@Injectable({ providedIn: 'root' })
export class TransfersService {
  private readonly api = inject(ApiService);

  solicitar(request: CrearTransferenciaRequest): Observable<Transferencia> {
    return this.api.post<Transferencia>('/transfers', request);
  }

  /** Seguimiento de las solicitudes iniciadas por el usuario (capitán destino). */
  enviadas(): Observable<Transferencia[]> {
    return this.api.get<Transferencia[]>('/transfers/enviadas');
  }
}
