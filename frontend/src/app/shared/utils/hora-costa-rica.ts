/**
 * Brakket muestra siempre hora de Costa Rica, sin importar dónde esté quien
 * mira la página.
 *
 * <p>El backend serializa sus fechas como reloj de pared sin zona
 * (`"2026-08-11T13:45"`). El navegador las interpreta como hora local y el
 * `DatePipe` las vuelve a formatear en hora local, así que las dos
 * conversiones se anulan y todo el mundo ve la hora que escribió el servidor.
 * Por eso las fechas <em>mostradas</em> ya salían bien en cualquier huso.</p>
 *
 * <p>Lo que no salía bien es el «ahora»: un `new Date()` devuelve el reloj del
 * navegador, que fuera de Costa Rica no es el mismo día ni la misma hora. Eso
 * hacía que un torneo de las 02:00 del 12 de agosto apareciera como empezado
 * desde Madrid mientras en Costa Rica todavía faltaban seis horas.</p>
 *
 * <p>Estas funciones devuelven el «ahora» en el mismo lenguaje que el resto de
 * la app: un `Date` cuyo reloj de pared local coincide con el de Costa Rica,
 * comparable directamente contra las fechas que llegan del API.</p>
 */

export const ZONA_COSTA_RICA = 'America/Costa_Rica';

/**
 * Momento actual expresado como reloj de pared de Costa Rica.
 *
 * <p>El `Date` que devuelve NO representa el instante correcto en UTC: sus
 * campos locales (`getHours`, `getDate`…) son los de Costa Rica. Eso es
 * justo lo que hace falta para compararlo con las fechas del backend, que
 * también son reloj de pared.</p>
 */
export function ahoraCostaRica(): Date {
  // `en-CA` da ISO (2026-08-11); `formatToParts` evita depender de cómo cada
  // navegador ordena los componentes al formatear a texto plano.
  const partes = new Intl.DateTimeFormat('en-CA', {
    timeZone: ZONA_COSTA_RICA,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).formatToParts(new Date());

  const campo = (tipo: Intl.DateTimeFormatPartTypes): number =>
    Number(partes.find((p) => p.type === tipo)?.value ?? 0);

  // `hour` puede venir como 24 a medianoche en algunos motores; el constructor
  // de Date lo normaliza al día siguiente, que es el comportamiento correcto.
  return new Date(
    campo('year'),
    campo('month') - 1,
    campo('day'),
    campo('hour'),
    campo('minute'),
    campo('second')
  );
}

/** Fecha de hoy en Costa Rica, como `YYYY-MM-DD`. */
export function hoyCostaRicaIso(): string {
  return isoDeFechaLocal(ahoraCostaRica());
}

/**
 * Pasa un `Date` a `YYYY-MM-DD` usando sus campos locales.
 *
 * <p>Nunca con `toISOString()`: ese convierte a UTC y en Costa Rica (-06:00)
 * devuelve el día siguiente a partir de las 18:00.</p>
 */
export function isoDeFechaLocal(fecha: Date): string {
  const dosDigitos = (n: number): string => String(n).padStart(2, '0');
  return [
    fecha.getFullYear(),
    dosDigitos(fecha.getMonth() + 1),
    dosDigitos(fecha.getDate())
  ].join('-');
}
