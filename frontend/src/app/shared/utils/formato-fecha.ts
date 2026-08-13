import { formatDate, registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';

/**
 * Formateo de fechas desde TypeScript, siempre con los datos de locale que
 * Angular empaqueta en el bundle: nunca con `Intl` ni con `toLocaleString`.
 *
 * <p>`Intl` y `toLocaleString` leen la base CLDR del navegador, que cambia con
 * el sistema operativo y con su versión: las abreviaturas de mes y los
 * separadores no están garantizados de un motor a otro. Peor todavía, `es-CR`
 * usa reloj de 12 horas, así que `toLocaleTimeString('es-CR', …)` devuelve
 * «02:45 p. m.» mientras el resto de la app muestra «14:45».</p>
 *
 * <p>`formatDate` usa los datos que registra `app.config.ts`, que viajan en el
 * bundle: la salida es idéntica en Windows, macOS y Linux, y en cualquier
 * navegador. Además es el mismo motor que el `DatePipe` de las plantillas, así
 * que una fecha formateada aquí y otra formateada en HTML coinciden.</p>
 */

/** Locale de la app. `app.config.ts` lo provee como LOCALE_ID. */
export const LOCALE_APP = 'es';

// El registro vive aquí, junto a quien lo necesita, y no solo en
// `app.config.ts`: los specs montan componentes con TestBed sin pasar por la
// configuración de la app, así que ahí los datos no estarían cargados y
// `formatDate` lanzaría NG0701. Registrar el mismo locale dos veces no tiene
// efecto adicional.
registerLocaleData(localeEs);

/** Valor que admiten los formateadores, igual que el `DatePipe`. */
type Fecha = string | number | Date;

/** «11 ago» — para listas donde el año se sobreentiende. */
export function diaMes(valor: Fecha): string {
  return formatDate(valor, 'd MMM', LOCALE_APP);
}

/** «11 ago 2026, 14:45» */
export function fechaHoraMedia(valor: Fecha): string {
  return formatDate(valor, 'd MMM y, HH:mm', LOCALE_APP);
}

/** «11/08/2026, 14:45» */
export function fechaHoraNumerica(valor: Fecha): string {
  return formatDate(valor, 'dd/MM/yyyy, HH:mm', LOCALE_APP);
}

/** «14:45» */
export function soloHora(valor: Fecha): string {
  return formatDate(valor, 'HH:mm', LOCALE_APP);
}
