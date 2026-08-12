import { ZONA_COSTA_RICA, ahoraCostaRica, hoyCostaRicaIso, isoDeFechaLocal } from './hora-costa-rica';

describe('hora-costa-rica', () => {
  /** Reloj de pared de Costa Rica ahora mismo, calculado por otra vía. */
  function referenciaCr(): { fecha: string; hora: string } {
    const texto = new Date().toLocaleString('sv-SE', { timeZone: ZONA_COSTA_RICA });
    return { fecha: texto.slice(0, 10), hora: texto.slice(11, 16) };
  }

  it('devuelve el reloj de pared de Costa Rica, no el del navegador', () => {
    const esperado = referenciaCr();
    const obtenido = ahoraCostaRica();

    expect(isoDeFechaLocal(obtenido)).toBe(esperado.fecha);
    const hora = `${String(obtenido.getHours()).padStart(2, '0')}:${String(obtenido.getMinutes()).padStart(2, '0')}`;
    expect(hora).toBe(esperado.hora);
  });

  it('hoyCostaRicaIso coincide con la fecha de Costa Rica', () => {
    expect(hoyCostaRicaIso()).toBe(referenciaCr().fecha);
  });

  it('isoDeFechaLocal usa los campos locales y no UTC', () => {
    // 31 de diciembre a las 20:00 locales. Con toISOString() en GMT-6 esto
    // daría 2027-01-01: la prueba fija que no ocurra.
    const fecha = new Date(2026, 11, 31, 20, 0, 0);

    expect(isoDeFechaLocal(fecha)).toBe('2026-12-31');
  });

  it('rellena mes y día a dos dígitos', () => {
    expect(isoDeFechaLocal(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('ahoraCostaRica es comparable con las fechas del API', () => {
    // Las fechas del backend llegan como reloj de pared sin zona; el Date que
    // produce el helper tiene que poder restarse contra ellas sin desfase.
    const ahora = ahoraCostaRica();
    const dentroDeUnaHora = new Date(ahora.getTime() + 3600_000);
    const haceUnaHora = new Date(ahora.getTime() - 3600_000);

    expect(dentroDeUnaHora > ahora).toBeTrue();
    expect(haceUnaHora < ahora).toBeTrue();
  });
});
