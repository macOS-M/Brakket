import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { TransmisionesRespuesta } from '../../../models/transmision.model';

/**
 * Datos de la página /transmisiones (RF-35). El frontend consume SOLO el
 * endpoint propio: el estado en vivo lo consulta el backend contra Helix
 * (con caché y credenciales que nunca llegan a este bundle, RNF-13).
 */
@Injectable({ providedIn: 'root' })
export class TransmisionesService {
  private readonly api = inject(ApiService);

  listar(): Observable<TransmisionesRespuesta> {
    return this.api.get<TransmisionesRespuesta>('/transmisiones');
  }
}
