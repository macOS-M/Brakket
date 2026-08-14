import {
  ANCHO,
  PAD,
  PuntoGrafico,
  altoInterno,
  cadenciaMs,
  crearEscalaX,
  crearEscalaY,
  decimarMinMax,
  dominioY,
  indiceMasCercano,
  pathDeArea,
  pathDeLinea,
  techoLindo,
} from './escalas';

describe('escalas', () => {
  const ALTO = 220;
  const MINUTO = 60_000;

  describe('techoLindo', () => {
    it('redondea al 1, 2 o 5 siguiente', () => {
      expect(techoLindo(7)).toBe(10);
      expect(techoLindo(150)).toBe(200);
      expect(techoLindo(1842)).toBe(2000);
      expect(techoLindo(0.4)).toBe(0.5);
    });

    it('no devuelve cero con entradas degeneradas', () => {
      expect(techoLindo(0)).toBe(1);
      expect(techoLindo(-5)).toBe(1);
      expect(techoLindo(NaN)).toBe(1);
    });
  });

  describe('dominioY', () => {
    it('ancla en cero y respeta un dominio fijo', () => {
      expect(dominioY([10, 90])).toEqual({ min: 0, max: 100 });
      expect(dominioY([0.2], { min: -1, max: 1 })).toEqual({ min: -1, max: 1 });
    });

    it('sobrevive a una serie sin valores', () => {
      expect(dominioY([null, null])).toEqual({ min: 0, max: 1 });
    });
  });

  describe('escalas', () => {
    it('centra el trazo cuando hay un solo punto', () => {
      const x = crearEscalaX([1000]);
      expect(x(1000)).toBe(PAD.left + (ANCHO - PAD.left - PAD.right) / 2);
      expect(Number.isNaN(x(1000))).toBeFalse();
    });

    it('reparte los extremos a lo ancho', () => {
      const x = crearEscalaX([0, 100]);
      expect(x(0)).toBe(PAD.left);
      expect(x(100)).toBe(ANCHO - PAD.right);
    });

    it('invierte la Y: el maximo va arriba', () => {
      const y = crearEscalaY({ min: 0, max: 100 }, ALTO);
      expect(y(100)).toBe(PAD.top);
      expect(y(0)).toBe(PAD.top + altoInterno(ALTO));
    });

    it('no divide por cero con una serie plana', () => {
      const y = crearEscalaY({ min: 5, max: 5 }, ALTO);
      expect(Number.isFinite(y(5))).toBeTrue();
    });
  });

  describe('cadenciaMs', () => {
    it('prefiere el intervalo que informa el backend', () => {
      expect(cadenciaMs([0, MINUTO], 30)).toBe(30_000);
    });

    it('cae en la mediana de los saltos cuando no viene', () => {
      expect(cadenciaMs([0, MINUTO, 2 * MINUTO, 10 * MINUTO], null)).toBe(MINUTO);
    });
  });

  describe('pathDeLinea', () => {
    const x = (t: number) => t;
    const y = (v: number) => v;

    it('une los puntos contiguos en un solo trazo', () => {
      const puntos: PuntoGrafico[] = [
        { t: 0, v: 10 },
        { t: MINUTO, v: 20 },
      ];
      const d = pathDeLinea(puntos, x, y, MINUTO);
      expect(d.match(/M/g)?.length).toBe(1);
      expect(d).toContain('L');
    });

    it('corta el trazo cuando el salto supera la cadencia', () => {
      const puntos: PuntoGrafico[] = [
        { t: 0, v: 10 },
        { t: 10 * MINUTO, v: 20 },
      ];
      expect(pathDeLinea(puntos, x, y, MINUTO).match(/M/g)?.length).toBe(2);
    });

    it('corta el trazo ante un hueco explicito', () => {
      const puntos: PuntoGrafico[] = [
        { t: 0, v: 10 },
        { t: MINUTO, v: null },
        { t: 2 * MINUTO, v: 20 },
      ];
      expect(pathDeLinea(puntos, x, y, MINUTO).match(/M/g)?.length).toBe(2);
    });

    it('devuelve vacio sin puntos', () => {
      expect(pathDeLinea([], x, y, MINUTO)).toBe('');
    });
  });

  describe('pathDeArea', () => {
    it('cierra el area contra la linea base', () => {
      const puntos: PuntoGrafico[] = [
        { t: 0, v: 10 },
        { t: MINUTO, v: 20 },
      ];
      const d = pathDeArea(puntos, (t) => t, (v) => v, MINUTO, 100);
      expect(d.endsWith('Z')).toBeTrue();
      expect(d).toContain('100.0');
    });
  });

  describe('decimarMinMax', () => {
    it('no toca series cortas', () => {
      const puntos: PuntoGrafico[] = [
        { t: 0, v: 1 },
        { t: 1, v: 2 },
      ];
      expect(decimarMinMax(puntos, 600)).toBe(puntos);
    });

    it('conserva el maximo global al reducir', () => {
      const puntos: PuntoGrafico[] = Array.from({ length: 5000 }, (_, i) => ({
        t: i * MINUTO,
        v: i === 3123 ? 9999 : i % 50,
      }));
      const reducidos = decimarMinMax(puntos, 470);
      expect(reducidos.length).toBeLessThan(puntos.length);
      expect(Math.max(...reducidos.map((p) => p.v ?? 0))).toBe(9999);
    });

    it('mantiene el orden temporal', () => {
      const puntos: PuntoGrafico[] = Array.from({ length: 2000 }, (_, i) => ({
        t: i,
        v: Math.sin(i),
      }));
      const reducidos = decimarMinMax(puntos, 100);
      const ordenados = [...reducidos].sort((a, b) => a.t - b.t);
      expect(reducidos).toEqual(ordenados);
    });
  });

  describe('indiceMasCercano', () => {
    it('encuentra la muestra mas proxima', () => {
      const instantes = [0, 10, 20, 30];
      expect(indiceMasCercano(instantes, 11)).toBe(1);
      expect(indiceMasCercano(instantes, 26)).toBe(3);
      expect(indiceMasCercano(instantes, -5)).toBe(0);
    });

    it('devuelve -1 sin instantes', () => {
      expect(indiceMasCercano([], 5)).toBe(-1);
    });
  });
});
