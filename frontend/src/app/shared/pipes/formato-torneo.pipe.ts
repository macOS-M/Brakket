import { Pipe, PipeTransform } from '@angular/core';

/**
 * El catálogo guarda el formato como código (DOBLE_ELIMINACION) y los torneos
 * como texto libre ("Eliminación directa"); acá se vuelve legible y en
 * español. Cualquier otro cae al genérico "guiones bajos → espacios, primera
 * en mayúscula".
 */
const NOMBRES: Record<string, string> = {
  ELIMINACION_DIRECTA: 'Eliminación directa',
  DOBLE_ELIMINACION: 'Doble eliminación',
  ROUND_ROBIN: 'Todos contra todos',
  SUIZO: 'Suizo',
  FASE_GRUPOS_Y_ELIMINACION: 'Fase de grupos y eliminación'
};

@Pipe({ name: 'formatoTorneo', standalone: true })
export class FormatoTorneoPipe implements PipeTransform {
  transform(formato: string | null | undefined): string {
    if (!formato) {
      return '';
    }
    // Se normaliza porque la misma etiqueta llega de dos fuentes: el código del
    // catálogo (ROUND_ROBIN) y el texto que guardó el torneo al crearse
    // ("Round robin"). Sin esto, los torneos viejos seguían mostrando el
    // nombre en inglés.
    const clave = formato
      .normalize('NFD')
      .replace(/\p{M}/gu, '')
      .toUpperCase()
      .replace(/\s+/g, '_');
    const conocido = NOMBRES[clave];
    if (conocido) {
      return conocido;
    }
    const texto = formato.replaceAll('_', ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}
