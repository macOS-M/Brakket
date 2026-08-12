import { FormatoTorneoPipe } from './formato-torneo.pipe';

describe('FormatoTorneoPipe', () => {
  const pipe = new FormatoTorneoPipe();

  it('traduce el código del catálogo', () => {
    expect(pipe.transform('ELIMINACION_DIRECTA')).toBe('Eliminación directa');
    expect(pipe.transform('DOBLE_ELIMINACION')).toBe('Doble eliminación');
    expect(pipe.transform('FASE_GRUPOS_Y_ELIMINACION')).toBe('Fase de grupos y eliminación');
    expect(pipe.transform('SUIZO')).toBe('Suizo');
  });

  it('muestra el round robin en español', () => {
    expect(pipe.transform('ROUND_ROBIN')).toBe('Todos contra todos');
  });

  it('traduce también los torneos que guardaron el nombre viejo en texto', () => {
    // Los torneos creados antes de traducir guardaron la etiqueta que se
    // mostraba entonces; sin normalizar, seguirían viéndose en inglés.
    expect(pipe.transform('Round robin')).toBe('Todos contra todos');
    expect(pipe.transform('round robin')).toBe('Todos contra todos');
  });

  it('deja intacto el nombre nuevo', () => {
    expect(pipe.transform('Todos contra todos')).toBe('Todos contra todos');
  });

  it('normaliza acentos al buscar el nombre conocido', () => {
    expect(pipe.transform('Eliminación directa')).toBe('Eliminación directa');
    expect(pipe.transform('eliminacion directa')).toBe('Eliminación directa');
  });

  it('un formato desconocido cae al genérico legible', () => {
    expect(pipe.transform('FORMATO_INVENTADO')).toBe('Formato inventado');
  });

  it('sin valor devuelve cadena vacía', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
    expect(pipe.transform('')).toBe('');
  });
});
