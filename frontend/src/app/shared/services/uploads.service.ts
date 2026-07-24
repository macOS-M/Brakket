import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApiService } from '../../core/services/api.service';

/** Subida directa de imágenes; el backend devuelve la URL pública. */
@Injectable({ providedIn: 'root' })
export class UploadsService {
  private readonly api = inject(ApiService);

  subirImagen(archivo: File): Observable<string> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.api.post<{ url: string }>('/uploads', cuerpo).pipe(map((r) => r.url));
  }
}
