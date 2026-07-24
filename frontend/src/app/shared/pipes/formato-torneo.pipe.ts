import { Pipe, PipeTransform } from '@angular/core';

/**
 * El catálogo guarda el formato como código (DOBLE_ELIMINACION); acá se
 * vuelve legible. Los conocidos llevan su acento; cualquier otro cae al
 * genérico "guiones bajos → espacios, primera en mayúscula".
 */
const NOMBRES: Record<string, string> = {
  ELIMINACION_DIRECTA: 'Eliminación directa',
  DOBLE_ELIMINACION: 'Doble eliminación',
  ROUND_ROBIN: 'Round robin',
  SUIZO: 'Suizo',
  FASE_GRUPOS_Y_ELIMINACION: 'Fase de grupos y eliminación'
};

@Pipe({ name: 'formatoTorneo', standalone: true })
export class FormatoTorneoPipe implements PipeTransform {
  transform(formato: string | null | undefined): string {
    if (!formato) {
      return '';
    }
    const conocido = NOMBRES[formato];
    if (conocido) {
      return conocido;
    }
    const texto = formato.replaceAll('_', ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}
