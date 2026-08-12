import { Pipe, PipeTransform } from '@angular/core';
import { etiquetaDeCodigo } from '../utils/etiquetas';

/**
 * Pinta un código de la base como texto legible.
 *
 * Uso: {{ transmision.estado | etiqueta }} → "En vivo" en vez de "EN_VIVO".
 *
 * La traducción vive en `etiquetas.ts` y no acá, para que también la puedan
 * usar los componentes que arman la etiqueta en TypeScript (StatusBadge).
 */
@Pipe({ name: 'etiqueta', standalone: true })
export class EtiquetaPipe implements PipeTransform {
  transform(codigo: string | null | undefined): string {
    return etiquetaDeCodigo(codigo);
  }
}
