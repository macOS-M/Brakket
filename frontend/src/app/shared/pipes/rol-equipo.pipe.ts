import { Pipe, PipeTransform } from '@angular/core';

/**
 * El backend guarda el rol como código (CAPITAN); acá se vuelve legible.
 * Cualquier rol futuro cae al genérico "primera en mayúscula".
 */
const NOMBRES: Record<string, string> = {
  CAPITAN: 'Capitán',
  TITULAR: 'Titular',
  SUPLENTE: 'Suplente',
  COACH: 'Coach'
};

@Pipe({ name: 'rolEquipo', standalone: true })
export class RolEquipoPipe implements PipeTransform {
  transform(rol: string | null | undefined): string {
    if (!rol) {
      return '';
    }
    const conocido = NOMBRES[rol];
    if (conocido) {
      return conocido;
    }
    const texto = rol.replaceAll('_', ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}
